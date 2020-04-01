package com.study.blog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 10652
 * <p>
 * 新权限认证方式：自定义注解，标记需要权限的方法
 * <p>
 * ElementType.METHOD：作用于方法
 * Retention.RUNTIME：作用于运行时
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateAnnotation {
    /**
     * 权限id：默认为管理员权限
     * <p>
     * 该字段为日后扩展使用！
     */
    int authorityId() default 1;

    /**
     * 用户名：用来验证正要执行的操作，是否是本人执行！
     * 例如：删除评论、添加分类等
     * 暂时，还没完成：先留着，慢慢完善
     */
    String username() default "user";
}
