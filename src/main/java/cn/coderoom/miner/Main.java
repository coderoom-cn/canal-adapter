package cn.coderoom.miner;

import com.alibaba.fastjson.JSONArray;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayList;
import java.util.List;


/** 
 * 
 * @class Main
 * @package cn.coderoom.miner
 * @author lim
 * @email coderoom.cn@gmail.com
 * @date 2019/8/3 13:37
*/ 
@EnableAspectJAutoProxy//开启AOP
@EnableAutoConfiguration //这个不能注释掉，必须的，组合注解内嵌全局com注解，注释的话扫描不到配置类
@ComponentScan
@SpringBootApplication
public class Main {

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


    /**
     * 项目入口
     *
     * java -jar canal-adapter.jar -c canal-adapter.json
     *
     * @param args
     * @throws ParseException
     */
    public static void main(String[] args) {

        //启动项目
        SpringApplication.run(Main.class);
    }

}
