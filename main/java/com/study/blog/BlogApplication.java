package com.study.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author 10652
 * 使用 mapperscan 注解 替代所有的 mapper 注解
 */
// @EnableRedisHttpSession
@SpringBootApplication
@MapperScan("com.study.blog.repository")
public class BlogApplication {
    public static void main(String[] args) {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
        SpringApplication.run(BlogApplication.class, args);
    }
}
