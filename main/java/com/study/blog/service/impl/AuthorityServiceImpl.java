package com.study.blog.service.impl;

import com.study.blog.entity.Authority;
import com.study.blog.repository.AuthorityMapper;
import com.study.blog.service.AuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 10652
 */
@Service
public class AuthorityServiceImpl implements AuthorityService {

    private final AuthorityMapper mapper;

    @Autowired
    AuthorityServiceImpl(AuthorityMapper mapper){
        this.mapper=mapper;
    }

    @Override
    public Authority getAuthorityById(Integer id) {
        return mapper.getAuthority(id);
    }
}
