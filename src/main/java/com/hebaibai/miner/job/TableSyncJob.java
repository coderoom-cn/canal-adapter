package com.hebaibai.miner.job;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.otter.canal.protocol.CanalEntry.*;

/**
 * 单表同步，表的字段名称可以不同，类型需要一致
 * 表中需要有id字段
 */
@SuppressWarnings("ALL")
@Slf4j
public class TableSyncJob extends Job {


    /**
     * 用于校验是否适用于当前的配置
     *
     * @param table
     * @param database
     * @return
     */
    @Override
    public boolean isMatches(String table, String database) {
        return prop.getString("database").equals(database) &&
                prop.getString("table").equals(table);
    }

    /**
     * 返回一个新的Runnable
     *
     * @param entry
     * @return
     */
    @Override
    public Runnable newTask(final Entry entry) {
        return () -> {
            RowChange rowChange = super.getRowChange(entry);
            if (rowChange == null) {
                return;
            }
            EventType eventType = rowChange.getEventType();
            int rowDatasCount = rowChange.getRowDatasCount();
            for (int i = 0; i < rowDatasCount; i++) {
                RowData rowData = rowChange.getRowDatas(i);
                if (eventType == EventType.DELETE) {
                    delete(rowData.getBeforeColumnsList());
                }
                if (eventType == EventType.INSERT) {
                    insert(rowData.getAfterColumnsList());
                }
                if (eventType == EventType.UPDATE) {
                    update(rowData.getBeforeColumnsList(), rowData.getAfterColumnsList());
                }
            }
        };
    }

    /**
     * 修改后的数据
     *
     * @param after
     */
    private void insert(List<Column> after) {
        //找到改动的数据
        List<Column> collect = after.stream().filter(column -> column.getUpdated() || column.getIsKey()).collect(Collectors.toList());
        //根据表映射关系拼装更新sql
        JSONObject mapping = prop.getJSONObject("mapping");
        String target = prop.getString("target");
        String sqlStr = "";
        if("epv_pv_users".equals(target)){
            sqlStr = getInsertEpvUser(mapping,target,collect);
        } else {
            sqlStr = getInsert(mapping,target,collect);
        }

        if("".equals(sqlStr)){
            return;
        }

        jdbcTemplate.execute(sqlStr);
    }

    /**
     * 更新数据
     *
     * @param before 原始数据
     * @param after  更新后的数据
     */
    private void update(List<Column> before, List<Column> after) {
        System.out.println(after.stream().collect(Collectors.toList()));
        //找到改动的数据
        List<Column> updataCols = after.stream().filter(column -> column.getUpdated()).collect(Collectors.toList());
        //找到之前的数据中的keys
        List<Column> keyCols = before.stream().filter(column -> column.getIsKey()).collect(Collectors.toList());
        //没有key,执行更新替换
        if (keyCols.size() == 0) {
            return;
        }
        //根据表映射关系拼装更新sql
        JSONObject mapping = prop.getJSONObject("mapping");
        String target = prop.getString("target");

        String sqlStr = "";
        if("epv_pv_users".equals(target)){
            sqlStr = getUpdateEpvSql(mapping ,target,updataCols,keyCols);
        } else {
            sqlStr = getUpdateSql(mapping ,target,updataCols,keyCols);
        }
        if("".equals(sqlStr)){
            return;
        }

        jdbcTemplate.execute(sqlStr);
    }

    /**
     * 删除数据
     *
     * @param before
     */
    private void delete(List<Column> before) {
        //找到改动的数据
        List<Column> keyCols = before.stream().filter(column -> column.getIsKey()).collect(Collectors.toList());
        if (keyCols.size() == 0) {
            return;
        }
        //根据表映射关系拼装更新sql
        JSONObject mapping = prop.getJSONObject("mapping");
        String target = prop.getString("target");
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM `").append(target).append("` WHERE ");
        List<String> where = new ArrayList<>();
        for (Column column : keyCols) {
            String name = mapping.getString(column.getName());
            where.add(name + " = '" + column.getValue() + "' ");
        }
        sql.append(StringUtils.join(where, "and "));
        String sqlStr = sql.toString();
        log.debug(sqlStr);
        jdbcTemplate.execute(sqlStr);
    }

    /**
     * 修改后的数据
     *
     * @param after
     */
    private String getInsertEpvUser(JSONObject mapping,String target,List<Column> collect) {

        List<String> columnNames = new ArrayList<>();
        List<String> columnValues = new ArrayList<>();
        for (int i = 0; i < collect.size(); i++) {
            Column column = collect.get(i);
            if (!mapping.containsKey(column.getName())) {
                continue;
            }
            String name = mapping.getString(column.getName());
            columnNames.add(name);
            if (column.getIsNull()) {
                columnValues.add("null");
            } else {
                columnValues.add("'" + column.getValue() + "'");
            }
        }
        StringBuilder sql = new StringBuilder();
        sql.append("REPLACE INTO ").append(target).append("( ")
                .append("user_id")
                .append(StringUtils.join(columnNames, ", "))
                .append(") VALUES ( ")
                .append("")
                .append(StringUtils.join(columnValues, ", "))
                .append(");");
        String sqlStr = sql.toString();
        log.debug(sqlStr);
        return sqlStr;
    }

    /**
     * 修改后的数据
     *
     * @param after
     */
    private String getInsert(JSONObject mapping,String target,List<Column> collect) {

        List<String> columnNames = new ArrayList<>();
        List<String> columnValues = new ArrayList<>();
        for (int i = 0; i < collect.size(); i++) {
            Column column = collect.get(i);
            if (!mapping.containsKey(column.getName())) {
                continue;
            }
            String name = mapping.getString(column.getName());
            columnNames.add(name);
            if (column.getIsNull()) {
                columnValues.add("null");
            } else {
                columnValues.add("'" + column.getValue() + "'");
            }
        }
        StringBuilder sql = new StringBuilder();
        sql.append("REPLACE INTO ").append(target).append("( ")
                .append(StringUtils.join(columnNames, ", "))
                .append(") VALUES ( ")
                .append(StringUtils.join(columnValues, ", "))
                .append(");");
        String sqlStr = sql.toString();
        log.debug(sqlStr);
        return sqlStr;
    }

    private String getUpdateSql(JSONObject mapping ,String target,List<Column> updataCols,List<Column> keyCols) {

        //待更新数据
        List<String> updatas = new ArrayList<>();
        for (int i = 0; i < updataCols.size(); i++) {
            Column updataCol = updataCols.get(i);
            if (!mapping.containsKey(updataCol.getName())) {
                log.info(" Not configured to change the field : {}"+updataCol.getName());
                continue;
            }
            String name = mapping.getString(updataCol.getName());
            if (updataCol.getIsNull()) {
                updatas.add("`" + name + "` = null");
            } else {
                updatas.add("`" + name + "` = '" + updataCol.getValue() + "'");
            }
        }
        //如果没有要修改的数据，返回
        if (updatas.size() == 0) {
            return "";
        }
        //keys
        List<String> keys = new ArrayList<>();
        for (Column keyCol : keyCols) {
            String name = mapping.getString(keyCol.getName());
            keys.add("`" + name + "` = '" + keyCol.getValue() + "'");
        }
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(target).append(" SET ");
        sql.append(StringUtils.join(updatas, ", "));
        sql.append(" WHERE ");
        sql.append(StringUtils.join(keys, "AND "));
        String sqlStr = sql.toString();
        log.info(" update sql : {}"+sqlStr);
        log.debug(sqlStr);
        return sqlStr;

    }

    private String getUpdateEpvSql(JSONObject mapping ,String target,List<Column> updataCols,List<Column> keyCols) {

        //待更新数据
        List<String> updatas = new ArrayList<>();
        for (int i = 0; i < updataCols.size(); i++) {
            Column updataCol = updataCols.get(i);
            if (!mapping.containsKey(updataCol.getName())) {
                log.info(" Not configured to change the field : {}"+updataCol.getName());
                continue;
            }
            String name = mapping.getString(updataCol.getName());
            if (updataCol.getIsNull()) {
                updatas.add("`" + name + "` = null");
            } else {
                updatas.add("`" + name + "` = '" + updataCol.getValue() + "'");
            }
        }
        //如果没有要修改的数据，返回
        if (updatas.size() == 0) {
            return "";
        }
        //keys
        List<String> keys = new ArrayList<>();
        for (Column keyCol : keyCols) {
            String name = mapping.getString(keyCol.getName());
            if("user_code".equals(name)){
                keys.add("`" + name + "` = '" + keyCol.getValue() + "'");
            }

        }
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(target).append(" SET ");
        sql.append(StringUtils.join(updatas, ", "));
        sql.append(" WHERE ");
        sql.append(StringUtils.join(keys, "AND "));
        String sqlStr = sql.toString();
        log.info(" update sql : {}"+sqlStr);
        log.debug(sqlStr);

        return sqlStr;
    }


}
