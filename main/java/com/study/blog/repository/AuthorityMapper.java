package com.study.blog.repository;

import com.study.blog.entity.Authority;

/**
 * @author 10652
 * 在启动类上添加 该注解 MapperScan("com.study.blog.repository") 替代该包下的所有 mapper 注解
 */
public interface AuthorityMapper {
    /**
     * 通过权限id获取权限名称
     *
     * @param id 权限id
     * @return 权限名称
     */
    Authority getAuthority(Integer id);
}
