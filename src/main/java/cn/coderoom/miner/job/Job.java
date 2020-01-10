package cn.coderoom.miner.job;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.alibaba.otter.canal.protocol.CanalEntry.Entry;

@Slf4j
@Data
public abstract class Job {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 数据库链接
     */
    protected JdbcTemplate jdbcTemplate;

    /**
     * 额外配置
     */
    protected JSONObject prop;

    /**
     * 校验目标是否为合适的数据库和表
     *
     * @param table
     * @param database
     * @return
     */
    abstract public boolean isMatches(String table, String database);

    /**
     * 实例化一个Runnable
     *
     * @param entry
     * @return
     */
    abstract public Runnable newTask(final Entry entry);


    /**
     * 获取RowChange
     *
     * @param entry
     * @return
     */
    protected CanalEntry.RowChange getRowChange(Entry entry) {
        try {
            return CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return null;
    }

}
