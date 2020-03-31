package com.study.blog.repository;

import com.study.blog.entity.ID;
import org.apache.ibatis.annotations.Param;

/**
 * @author 10652
 */
public interface IDRepository {
    ID getUserId(@Param("blogId") Long blogId, @Param("commentId") Long commentId);
}
