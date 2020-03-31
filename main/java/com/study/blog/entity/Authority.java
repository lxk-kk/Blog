package com.study.blog.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

/**
 * 权限：包括 权限id 以及 权限名称
 *
 * @author 10652
 */
@Getter
@Setter
public class Authority implements GrantedAuthority {

    private static final long serialVersionUID = -6929828092640857122L;

    private Integer authorityId;
    private String authorityName;

    @Override
    public String getAuthority() {
        return authorityName;
    }

    public Integer getAuthorityId() {
        return this.authorityId;
    }

    /**
     * 当任何时候获取到Authority对象时，都可以获取到对应的 角色名称
     *
     * @return name
     */
    @Override
    public String toString() {
        return authorityName;
    }
}
