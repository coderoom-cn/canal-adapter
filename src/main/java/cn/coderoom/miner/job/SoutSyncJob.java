package cn.coderoom.miner.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.alibaba.otter.canal.protocol.CanalEntry.*;

@Slf4j
public class SoutSyncJob extends Job {

    protected static String row_format = null;
    protected static final String SEP = SystemUtils.LINE_SEPARATOR;
    protected static String context_format = null;
    protected static String transaction_format = null;

    static {
        context_format = SEP + "****************************************************" + SEP;
        context_format += "* Batch Id: [{}] ,count : [{}] , memsize : [{}] , Time : {}" + SEP;
        context_format += "* Start : [{}] " + SEP;
        context_format += "* End : [{}] " + SEP;
        context_format += "****************************************************" + SEP;
        row_format = SEP
                + "----------------> binlog[{}:{}] , name[{},{}] , eventType : {} , executeTime : {}({}) , gtid : ({}) , delay : {} ms"
                + SEP;
        transaction_format = SEP
                + "================> binlog[{}:{}] , executeTime : {}({}) , gtid : ({}) , delay : {}ms"
                + SEP;

    }

    @Override
    public boolean isMatches(String table, String database) {
        return true;
    }

    @Override
    public Runnable newTask(final Entry entry) {
        return () -> {
            printEntry(entry);
        };
    }

    /**
     * 打印实体
     *
     * @param entry
     */
    private void printEntry(Entry entry) {
        long executeTime = entry.getHeader().getExecuteTime();
        long delayTime = new Date().getTime() - executeTime;
        Date date = new Date(entry.getHeader().getExecuteTime());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (entry.getEntryType() == EntryType.ROWDATA) {
            RowChange rowChage = super.getRowChange(entry);
            EventType eventType = rowChage.getEventType();
            Header header = entry.getHeader();
            System.out.println(">>>>>>>>>>>>>>>"+header.getTableName());
            log.info(row_format,
                    new Object[]{entry.getHeader().getLogfileName(),
                            String.valueOf(entry.getHeader().getLogfileOffset()), entry.getHeader().getSchemaName(),
                            entry.getHeader().getTableName(), eventType,
                            String.valueOf(entry.getHeader().getExecuteTime()), simpleDateFormat.format(date),
                            entry.getHeader().getGtid(), String.valueOf(delayTime)});

            if (eventType == EventType.QUERY || rowChage.getIsDdl()) {
                log.info(" sql ----> " + rowChage.getSql() + SEP);
                return;
            }

            printXAInfo(rowChage.getPropsList());
            for (RowData rowData : rowChage.getRowDatasList()) {
                if (eventType == EventType.DELETE) {
                    printColumn(rowData.getBeforeColumnsList());
                } else if (eventType == EventType.INSERT) {
                    printColumn(rowData.getAfterColumnsList());
                } else {
                    printColumn(rowData.getAfterColumnsList());
                }
            }
        }
    }

    private void printXAInfo(List<Pair> pairs) {
        if (pairs == null) {
            return;
        }

        String xaType = null;
        String xaXid = null;
        for (Pair pair : pairs) {
            String key = pair.getKey();
            if (StringUtils.endsWithIgnoreCase(key, "XA_TYPE")) {
                xaType = pair.getValue();
            } else if (StringUtils.endsWithIgnoreCase(key, "XA_XID")) {
                xaXid = pair.getValue();
            }
        }

        if (xaType != null && xaXid != null) {
            log.info(" ------> " + xaType + " " + xaXid);
        }
    }

    private void printColumn(List<Column> columns) {
        for (Column column : columns) {
            StringBuilder builder = new StringBuilder();
            try {
                if (StringUtils.containsIgnoreCase(column.getMysqlType(), "BLOB")
                        || StringUtils.containsIgnoreCase(column.getMysqlType(), "BINARY")) {
                    // get value bytes
                    builder.append(column.getName() + " : "
                            + new String(column.getValue().getBytes("ISO-8859-1"), "UTF-8"));
                } else {
                    builder.append(column.getName() + " : " + column.getValue());
                }
            } catch (UnsupportedEncodingException e) {
            }
            builder.append("    type=" + column.getMysqlType());
            if (column.getUpdated()) {
                builder.append("    update=" + column.getUpdated());
            }
            builder.append(SEP);
            log.info("printColumn ------> "+builder.toString());
        }
    }


}
