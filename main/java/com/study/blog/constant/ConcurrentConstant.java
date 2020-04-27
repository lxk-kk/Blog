package com.study.blog.constant;

/**
 * 并发限流常量类
 * 对于数值量而言，可以动态优化
 *
 * @author 10652
 */
public class ConcurrentConstant {

    /**
     * 默认允许 512 个线程获取许可证（允许访问数据库）
     */
    public static final Integer ALLOW_PERMISSION = 1024;

    /**
     * 默认允许 1024 个线程等待获取许可证（等待访问数据库）：
     */
    public static final Integer LIMIT_FLOW = 1024;

    /**
     * 默认每个线程 被阻塞 1 分钟（单位：纳秒）
     */
    public static final long WAIT_TIMEOUT_N = 60000000000L;

    /**
     * 默认每个线程 被阻塞 1 分钟（单位：毫秒秒）
     */
    public static final long WAIT_TIMEOUT_MILLS = 60000L;

    /**
     * 默认每个线程 被阻塞 1 分钟（单位：分钟）
     */
    public static final long WAIT_TIMEOUT_M = 1L;

    /**
     * 默认自旋次数
     */
    public static final int SPIN_TIME = 4;

    /**
     * 错误提示信息
     */
    public static final String SYSTEM_BUSY_MSG = "系统正忙，请稍后再试";

}
