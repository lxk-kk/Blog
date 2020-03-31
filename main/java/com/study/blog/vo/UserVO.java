package com.study.blog.vo;

import com.study.blog.entity.Authority;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author 10652
 */
@Data
public class UserVO {
    private Integer id;
    /**
     * 用户名
     * 对用户名进行验证，并且用于名为长度为2~3
     */
    @NotBlank(message = "姓名不可为空")
    @Size(min = 2, max = 3)
    private String name;
    /**
     * 用户邮箱
     * 验证是否为空，限制邮箱长度，验证是否为电子邮箱（格式）
     */
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 50)
    @Email(message = "邮箱格式不正确")
    private String email;
    /**
     * 用户账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 20)
    private String username;

    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 用户权限列表
     */
    private List<Authority> authorities;
}
