package com.study.blog.service.impl;

import com.study.blog.entity.ID;
import com.study.blog.repository.IDRepository;
import com.study.blog.service.IDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IDServiceImpl implements IDService {
    private final IDRepository repository;

    @Autowired
    public IDServiceImpl(IDRepository repository) {
        this.repository = repository;
    }

    @Override
    public ID getUserId(Long blogId, Long commentId) {
        return repository.getUserId(blogId, commentId);
    }
}
