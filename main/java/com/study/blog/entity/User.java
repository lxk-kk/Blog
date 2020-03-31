package com.study.blog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * @author 10652
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class User implements UserDetails, Serializable {

    /**
     * 用户id：唯一标识（自增策略：在mapper.xml文件中设置useGeneratedKeys=true即可）
     * 并且设置keyProperty 属性为 id 使得自增的主键值回填
     */
    private static final long serialVersionUID = 1510568160218552391L;
    private Integer id;
    /**
     * 用户名
     * 对用户名进行验证，并且用于名为长度为2~3
     */
    @NotBlank(message = "请填写姓名")
    @Size(max = 15, message = "请正确填写姓名")
    private String name;
    /**
     * 用户邮箱
     * 验证是否为空，限制邮箱长度，验证是否为电子邮箱（格式）
     */
    @NotBlank(message = "请填写邮箱")
    @Size(max = 50, message = "请正确填写邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;
    /**
     * 用户账号
     */
    @NotBlank(message = "请填写账号")
    @Size(min = 3, max = 15, message = "账号最短 3 个字，最长 5 个字")
    private String username;
    /**
     * 用户密码：序列化的时候忽略密码！不把密码传递给前端
     */
    @JsonIgnore
    @NotBlank(message = "请填写密码")
    @Size(max = 100, message = "密码长度超过限制")
    private String password;

    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 用户权限列表
     */
    private List<Authority> authorities;

    /**
     * 需要实现该方法：在获取 Authority 用户角色时，能够返回角色的名称
     * @return 角色列表
     */
   /* @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 需要将List<Authority>转换成List<SimpleGrantedAuthority>，否则前端拿不到角色里列表名称
        List<SimpleGrantedAuthority> authorityList = new ArrayList<>(1);
        log.info("authorities：{}", this.authorities);
        log.info("username：{}", this.username);
        for (Authority auth : authorities) {
            authorityList.add(new SimpleGrantedAuthority(auth.getAuthority()));
        }
        return authorityList;
    }*/

    /**
     * 为 authorities 单独新建一个get方法，获取原生的Authority对象列表，不知道为什么，上面个方法只能获取到authorityName，而获取不到authorityId
     *
     * @return Authority
     */
    public List<Authority> getOriginAuthorities() {
        return this.authorities;
    }

    public void setEncodePassword(String password) {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        this.password = encoder.encode(password);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
