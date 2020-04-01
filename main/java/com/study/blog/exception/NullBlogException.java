package com.study.blog.exception;

/**
 * blog 不存在异常
 *
 * @author 10652
 */
public class NullBlogException extends RuntimeException {
    int status;
    public NullBlogException(String message) {
        super(message);
    }
}
