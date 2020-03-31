package com.study.blog.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 10652
 */
@Slf4j
@Data
public class ID {
    Integer blogUserId;
    Integer commentUserId;
    Long blogId;
    Long commentId;
}
