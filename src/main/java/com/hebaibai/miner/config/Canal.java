package com.hebaibai.miner.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.hebaibai.miner.CanalConf;
import com.hebaibai.miner.CanalTask;
import com.hebaibai.miner.DataSourceConf;
import com.hebaibai.miner.job.Job;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ResourceUtils;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @package：com.hebaibai.miner.config
 * @description:
 * @author: Leesire
 * @email:coderoom.cn@gmail.com
 * @createtime: 2019/7/9
 */
@Configuration
@Slf4j
public class Canal {

    //canal链接配置
    private static CanalConf canalConf;
    //应用名称
    private static String appName;
    //线程池大小设置
    private static int threadPoolSize;
    //数据库配置
    private static List<DataSourceConf> dataSourceConfs = new ArrayList<>();
    //jobs配置
    private static JSONArray jobs = null;


    @Bean
    public void init(){

        File file = null;
        try {
            file = ResourceUtils.getFile("classpath:canal-adapter.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //try (FileInputStream inputStream = new FileInputStream(getConf(args))) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            JSONObject jsonObject = JSONObject.parseObject(new String(bytes, "utf-8"));
            //配置设置
            appName = jsonObject.getString("appName");
            threadPoolSize = jsonObject.getInteger("threadPoolSize");
            //canal配置
            canalConf = jsonObject.getObject("canal", CanalConf.class);
            canalConf.setDestination(appName);
            //数据库配置
            JSONObject dataSource = jsonObject.getJSONObject("dataSource");
            for (String key : dataSource.keySet()) {
                DataSourceConf dataSourceConf = dataSource.getObject(key, DataSourceConf.class);
                dataSourceConf.setName(key);
                dataSourceConfs.add(dataSourceConf);
            }
            //jod配置
            jobs = jsonObject.getJSONArray("jobs");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
            return;
        }

    }
    /**
     * 线程池
     * 大小为处理器核心数×2
     *
     * @return
     */
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * 任务集合
     *
     * @return
     */
    @Bean("jobList")
    public List<Job> jobList(
            @Autowired @Qualifier("dataSourceMap") Map<String, DataSource> dataSourceMap
    ) {
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < this.jobs.size(); i++) {
            JSONObject jobJson = this.jobs.getJSONObject(i);
            //实例化
            String className = jobJson.getString("className");
            //对应数据库
            String dataSource = jobJson.getString("dataSource");
            //额外配置
            JSONObject prop = jobJson.getJSONObject("prop");
            try {
                Class<?> aClass = Class.forName(className);
                Job instance = (Job) aClass.newInstance();
                if (!dataSourceMap.containsKey(dataSource)) {
                    System.err.println(dataSource + "没有配置");
                    System.exit(0);
                }
                instance.setJdbcTemplate(new JdbcTemplate(dataSourceMap.get(dataSource)));
                instance.setProp(prop);
                jobs.add(instance);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
        return jobs;
    }

    /**
     * 所有的数据库配置
     *
     * @return
     * @throws PropertyVetoException
     */
    @Bean("dataSourceMap")
    public Map<String, DataSource> dataSourceMap() throws PropertyVetoException {
        Map<String, DataSource> dataSourceMap = new TreeMap<>();
        for (DataSourceConf dataSourceConf : dataSourceConfs) {
            ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
            comboPooledDataSource.setDriverClass(dataSourceConf.getDriverClass());
            comboPooledDataSource.setJdbcUrl(dataSourceConf.getUrl());
            comboPooledDataSource.setUser(dataSourceConf.getUsername());
            comboPooledDataSource.setPassword(dataSourceConf.getPassword());
            dataSourceMap.put(dataSourceConf.getName(), comboPooledDataSource);
        }
        return dataSourceMap;
    }

    @Bean
    public CanalConnector canalConnector() {
        CanalConnector connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(canalConf.getIp(), canalConf.getPort()),
                canalConf.getDestination(),
                canalConf.getUser(),
                canalConf.getPassword()
        );
        return connector;
    }

    /**
     * 任务线程
     *
     * @return
     */
    @Bean
    public CanalTask canalTask(
            @Autowired CanalConnector canalConnector,
            @Autowired @Qualifier("jobList") List<Job> jobList,
            @Autowired ExecutorService executorService
    ) {
        CanalTask canalTask = new CanalTask();
        canalTask.setBatchSize(threadPoolSize);
        canalTask.setConnector(canalConnector);
        canalTask.setDaemon(true);
        canalTask.setName(appName);
        canalTask.setJobList(jobList);
        canalTask.setExecutorService(executorService);
        canalTask.start();
        return canalTask;
    }

    /**
     * 获取配置文件
     *
     * @param args
     * @return
     * @throws ParseException
     */
    private static File getConf(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("c", "conf", true, "config file path");
        CommandLine parse = new BasicParser().parse(options, args);
        if (!parse.hasOption("conf")) {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("Options", options);
            System.exit(0);
        }
        String conf = parse.getOptionValue("conf");
        String path = System.getProperty("user.dir");
        return new File(path + "/" + conf);
    }

}
