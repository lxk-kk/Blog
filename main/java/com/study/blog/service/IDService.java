package com.study.blog.service;

import com.study.blog.entity.ID;

public interface IDService {
    ID getUserId(Long blogId, Long commentId);
}
