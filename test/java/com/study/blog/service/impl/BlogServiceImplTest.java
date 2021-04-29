package com.study.blog.service.impl;

import com.study.blog.service.BlogService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class BlogServiceImplTest {
    @Autowired
    BlogService service;

    @Test
    public void getBlogById() throws Exception {
        // service.getBlogById(13L);
    }

}