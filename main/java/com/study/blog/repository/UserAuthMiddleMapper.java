package com.study.blog.repository;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 10652
 * 在启动类上添加 该注解 MapperScan("com.study.blog.repository") 替代该包下的所有 mapper 注解
 */
@Repository
public interface UserAuthMiddleMapper {
    /**
     * 中间表维护多对多关系映射
     *
     * @param userId  用户id
     * @param authIds 权限id列表
     */
    void saveUserAuth(@Param("userId") Integer userId, @Param("authIds") List<Integer> authIds);
}
