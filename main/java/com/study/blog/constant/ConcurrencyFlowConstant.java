package com.study.blog.constant;

/**
 * @author 10652
 */
public class ConcurrencyFlowConstant {

    /**
     * 默认允许 1000 个不同的 博客请求 转发到数据库
     */
    public static final Integer LIMIT_FLOW_SIZE = 1024;


    /**
     * 默认每个线程 被阻塞 5 分钟！
     */
    public static final long WAIT_TIMEOUT = 2700000000000L;

    public static final String SYSTEM_BUSY_MSG = "系统正忙，请稍后再试";

}
