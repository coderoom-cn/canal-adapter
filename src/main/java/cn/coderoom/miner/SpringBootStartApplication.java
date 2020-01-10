package cn.coderoom.miner;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 *
 * @class SpringBootStartApplication
 * @package cn.coderoom.miner
 * @author lim
 * @email coderoom.cn@gmail.com
 * @date 2019/7/9 15:37
*/
public class SpringBootStartApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // 注意这里一定要指向原先用main方法执行的Application启动类
        return builder.sources(Main.class);
    }

}
