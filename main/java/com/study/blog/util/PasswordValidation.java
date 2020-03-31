package com.study.blog.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * @author 10652
 */
@Slf4j
@Component
public class PasswordValidation {

    private static PasswordEncoder passwordEncoder;
    private final PasswordEncoder passwordEncoderTemp;

    /**
     * 需要在静态方法使用 bean ：这是不被允许的，我们只有通过“欺骗”的方式满混过关
     * 首先：注入一个 bean （非静态）
     * 然后：将注入的 bean 赋值给静态的 实例变量
     * 这样就能在静态方法中使用 注入的bean 了
     *
     * @param passwordEncoderTemp 临时的非静态 bean
     */
    @Autowired
    public PasswordValidation(PasswordEncoder passwordEncoderTemp) {
        this.passwordEncoderTemp = passwordEncoderTemp;
    }

    /**
     * 验证密码是否被修改过
     *
     * @param originPassword 旧的密码序列
     * @param newPassword    新密码
     * @return 是否被修改过
     */
    @Deprecated
    public static Boolean validatePassword(String originPassword, String newPassword) {
        return Objects.isNull(newPassword) || passwordEncoder.matches(originPassword, passwordEncoder.encode
                (newPassword));
    }

    /**
     * 验证密码是否被修改过：等值验证即可：用户管理员修改用户信息时验证密码是否被修改
     *
     * @param p1 originPassword
     * @param p2 password
     * @return true/false
     */
    public static Boolean equalPassword(String p1, String p2) {
        return Objects.equals(p1, p2);
    }

    @PostConstruct
    void init() {
        passwordEncoder = this.passwordEncoderTemp;
    }
}
