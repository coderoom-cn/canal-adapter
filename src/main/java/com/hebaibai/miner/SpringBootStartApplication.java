package com.hebaibai.miner;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * @package：com.hebaibai.miner
 * @description:
 * @author: Leesire
 * @email:coderoom.cn@gmail.com
 * @createtime: 2019/7/9
 */
public class SpringBootStartApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // 注意这里一定要指向原先用main方法执行的Application启动类
        return builder.sources(Main.class);
    }

}
