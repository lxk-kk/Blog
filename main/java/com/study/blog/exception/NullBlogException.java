package com.study.blog.exception;

/**
 * blog 不存在异常
 *
 * @author 10652
 */
public class NullBlogException extends RuntimeException {
    private static final long serialVersionUID = -8807349809491400975L;
    int status;
    public NullBlogException(String message) {
        super(message);
    }
}
