package com.study.blog.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class BlogEvaluationCacheServiceImplTest {
    @Autowired
    BlogEvaluationCacheServiceImpl service;


    @Test
    public void test1() throws Exception {
        // System.out.println(service.test(13L));
    }

}