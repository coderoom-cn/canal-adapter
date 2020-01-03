package com.hebaibai.miner;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.Message;
import com.hebaibai.miner.job.Job;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import static com.alibaba.otter.canal.protocol.CanalEntry.Header;

/**
 * 接受binlog，分发任务
 * @class CanalTask
 * @package com.hebaibai.miner
 * @author lim
 * @email coderoom.cn@gmail.com
 * @date 2019/8/3 13:44
*/
@Slf4j
public class CanalTask extends Thread {

    @Setter
    private CanalConnector connector;

    @Setter
    private ExecutorService executorService;

    @Setter
    private List<Job> jobList;

    /**
     * 每次获取binlog消息数量，大小是线程池大小的3倍
     */
    @Setter
    private int batchSize;

    /**
     * 线程运行标示
     */
    private boolean run = false;

    /**
     * 启动链接
     */
    public void startCanal() {
        connector.connect();
        connector.subscribe();
        run = true;
    }

    /**
     * 停止链接
     */
    public void stopCanal() {
        run = false;
        connector.disconnect();
    }

    /**
     * 退出项目
     */
    public void exit() {
        stopCanal();
        System.err.println("bye.");
        System.exit(0);
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!run) {
                    startCanal();
                }
            } catch (Exception e) {
                System.err.println("连接失败，尝试重新链接。。。");
                log.info("Connection failed, try to relink。。。");
                threadSleep(3 * 1000);
            }
            System.err.println("链接成功。。。");
            log.info("Successful link。。。");
            try {
                while (run) {
                    Message message = connector.getWithoutAck(batchSize * 3);
                    long id = message.getId();
                    process(message);
                    connector.ack(id);
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } finally {
                stopCanal();
            }
        }

    }


    /**
     *  处理binlog中的消息
     * @param message
     * @author lim
     * @date 2020/1/3 13:45
     * @return void
    */
    void process(Message message) {
        List<Entry> entries = message.getEntries();
        if (entries.size() <= 0) {
            return;
        }
        log.info("process message.entries.size:{}", entries.size());
        for (Entry entry : entries) {
            Header header = entry.getHeader();
            String tableName = header.getTableName();
            String schemaName = header.getSchemaName();
            log.info("tableName:{}", tableName);
            log.info("schemaName:{}", schemaName);
            if (StringUtils.isAllBlank(tableName, schemaName)) {
                continue;
            }
            jobList.stream()
                    .filter(job -> job.isMatches(tableName, schemaName))
                    .forEach(job -> executorService.execute(job.newTask(entry)));
        }
    }

    /**
     * 线程休眠
     *
     * @param s
     */
    void threadSleep(long s) {
        try {
            Thread.sleep(s);
        } catch (InterruptedException e) {
        }
    }

}
