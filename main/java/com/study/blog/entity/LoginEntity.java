package com.study.blog.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author 10652
 *
 * 登录实体：账号、密码
 */
@Data
public class LoginEntity {
    @NotBlank(message = "用户或密码错误")
    String username;
    @NotBlank(message = "用户或密码错误")
    String password;
}
