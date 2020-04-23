package com.study.blog.exception;

/**
 * @author 10652
 */
public class LimitFlowException extends RuntimeException {
    private static final long serialVersionUID = 2919933103249959139L;
    int status;

    public LimitFlowException(String message) {
        super(message);
    }
}
