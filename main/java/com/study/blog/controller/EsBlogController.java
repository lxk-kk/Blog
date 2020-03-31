/*
package com.study.blog.controller;

import com.study.blog.entity.EsBlog;
import com.study.blog.repository.EsBlogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

*/
/**
 * @author 10652
 *//*

@Slf4j
@RestController
@RequestMapping("/blog")
public class EsBlogController {
    private final EsBlogRepository repository;

    @Autowired
    public EsBlogController(EsBlogRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/search")
    public List<EsBlog> search(@RequestParam("title") String title,
                               @RequestParam("content") String content,
                               @RequestParam("summary") String summary,
                               @RequestParam(value = "pageSize", defaultValue = "1") int pageSize,
                               @RequestParam(value = "pageIndex", defaultValue = "10") int pageIndex) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Page<EsBlog> blog = null;
        return blog.getContent();
    }

    @GetMapping("/list")
    public List<EsBlog> list() {
        Page<EsBlog> blog = repository.findAll(PageRequest.of(0, 10));
        return blog.getContent();
    }
}
*/
