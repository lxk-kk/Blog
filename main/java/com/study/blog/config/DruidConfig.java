package com.study.blog.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author 10652
 */
@Configuration
public class DruidConfig {
    /**
     * 将DruidDataSource的实例bean作为DataSource的实例bean注入到ioc容器中：表示自定义的数据源
     * 将配置文件中前缀为spring.datasource的配置全部绑定到DruidDataSource的实例bean上
     *
     * @return DruidDataSource实例bean
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource druidDataSource() {
        return new DruidDataSource();
    }

    /**
     * 接下来：配置监控后台的servlet处理所有后台请求
     * 注入一个Servlet：则需要将目标servlet配置进ServletRegistrationBean，在将该类的bean返回
     */
    @Bean
    public ServletRegistrationBean<StatViewServlet> statViewServlet() {
        // 设置该servlet处理所有druid路径下的请求
        ServletRegistrationBean<StatViewServlet> bean = new ServletRegistrationBean<>(new StatViewServlet(),
                "/druid/*");
        //  可初始化的的属性：在ResourceServlet可查看：StatViewServlet实现了该抽象类
        // 白名单
        bean.addInitParameter("allow", "");
        bean.addInitParameter("loginUsername", "admin");
        bean.addInitParameter("loginPassword", "admin");
        // bean.addInitParameter("resetEnable", "false");
        return bean;
    }

    /**
     * 接下来：配置过滤器：过滤请求
     * 注入一个Filter：将目标filter配置进FilterRegistrationBean，再将该类的bean返回即可
     */
    @Bean
    public FilterRegistrationBean webStatFilter() {
        FilterRegistrationBean<WebStatFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new WebStatFilter());
        // 可初始化的属性在WebStatFilter类中查看
        //过滤所有请求
        bean.addUrlPatterns("/*");
        bean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico");

        return bean;
    }
    // 接下来：启动应用：访问localhost:8080/druid/login即可查看后台监控
}
