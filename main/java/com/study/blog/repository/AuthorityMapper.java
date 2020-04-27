package com.study.blog.repository;

import com.study.blog.entity.Authority;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 10652
 * 在启动类上添加 该注解 MapperScan("com.study.blog.repository") 替代该包下的所有 mapper 注解
 */
@Repository
public interface AuthorityMapper {
    /**
     * 通过权限id获取权限名称
     *
     * @param id 权限id
     * @return 权限名称
     */
    Authority getAuthority(@Param("id") Integer id);


    /**
     * 根据 authority id 列表，获取 authority list
     *
     * @param authorityIdList id list
     * @return authority list
     */
    List<Authority> getAuthorityList(List<Integer> authorityIdList);
}
