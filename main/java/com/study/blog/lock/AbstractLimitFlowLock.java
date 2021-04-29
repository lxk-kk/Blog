package com.study.blog.lock;

import com.study.blog.constant.ConcurrentConstant;
import com.study.blog.exception.NullBlogException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【限流锁】
 * 1. Semaphore 实现流量限制
 * 2. 借助唯一的 blogId 作为锁标志，将其存储在 HashMap 中
 * 3. 通过锁标志阻塞后续相同 博客 请求的线程
 * <p>
 * 如果线程请求的 blogId，在 HashMap 中不存在，则它将持有锁，主要负责如下工作：
 * （1）负责创建 锁标志
 * （2）负责访问数据库，刷新缓存
 * （3）负责唤醒被该博客阻塞的所有线程
 * <p>
 * 如果线程请求的 blogId，已经存在于 HashMap 中，则它将被阻塞，主要执行如下行为：
 * （1）将自己阻塞
 * （2）被唤醒（超时自动苏醒）之后，查询缓存，取走数据
 *
 * @author 10652
 */
@Slf4j
public abstract class AbstractLimitFlowLock {
    /**
     * 限制流量
     */
    private Semaphore allowRequest;
    /**
     * 优化策略：决定 线程 阻塞之前的自旋次数
     */
    private int spinTime;


    AbstractLimitFlowLock() {
        this.allowRequest = new Semaphore(ConcurrentConstant.ALLOW_PERMISSION);
        spinTime = ConcurrentConstant.SPIN_TIME;
    }


    /**
     * 限流接口
     * 1. allowOrWait 判断当前线程是否有资格获取许可证
     * （1）有资格获取许可证：加锁（阻塞后续相同请求的线程），返回 true
     * （2）没有资格获取许可证：阻塞线程，当线程超时苏醒/被唤醒 之后，返回 false
     * <p>
     * 2. dispatchPermission 分发许可证
     * （1）成功获取许可证（包括阻塞等待后获得到许可证）：返回 true
     * （2）取消获取许可证（获取前检测到缓存已刷新 / 流量达到限制）：返回 false
     * <p>
     * 注意：如果取消获取许可证，则应该释放锁，并唤醒被本线程阻塞的其他线程
     *
     * @param blogId       blogId
     * @param makeSureLock 函数式接口：判断是否确认加锁
     * @return true：加锁成功，false：线程被阻塞唤醒
     */
    public boolean limitRequestPass(Long blogId, MakeSureLock makeSureLock, boolean spinningBlock) {
        if (allowOrWait(blogId, makeSureLock, spinningBlock)) {
            try {
                if (dispatchPermission(makeSureLock)) {
                    log.info("【获取许可证】thread:{}，BlogID:{}，剩余:{}",
                            Test.thisThread(), blogId, allowRequest.availablePermits());
                    // Thread.sleep(10000);
                    return true;
                }
            } catch (InterruptedException e) {
                log.error("【获取许可证】等待获取被中断");
            } catch (Exception sureLockException) {
                log.info("【获取许可证】获取前 SureLock 异常");
                releaseLock(blogId);
                throw sureLockException;
            }
            releaseLock(blogId);
        }
        return false;
    }

    /**
     * 释放许可证
     * <p>
     * 获得许可证的线程访问完数据库之后，就应该立即释放许可证，提高系统的吞吐量！
     */
    public void releasePermission() {
        allowRequest.release();
    }

    /**
     * 释放锁
     * <p>
     * 持有锁的线程刷新缓存之后，应该立即释放持有的锁！
     *
     * @param blogId blogId
     */
    public void releaseLock(Long blogId) {
        unLock(blogId);
    }

    /**
     * 分发许可证：
     * 1. queueLength：如果当前阻塞的线程数 达到 限制的数量：限流（返回 false）！
     * 2. sureLock：如果当前缓存已更新，则不再需要获取许可证（返回 false）！
     * 3. tryAcquire(timeout)：尝试获取许可证：tryAcquire
     * （1）成功获取：返回 true
     * （2）超时未获取：返回 false
     *
     * @param makeSureLock 确认是否加锁
     * @return true：成功 获取 许可证，false：不获取许可证
     * @throws InterruptedException 阻塞中断
     */
    private boolean dispatchPermission(MakeSureLock makeSureLock) throws InterruptedException {
        return allowRequest.getQueueLength() <= ConcurrentConstant.LIMIT_FLOW
                && makeSureLock.sureLock()
                && allowRequest.tryAcquire(ConcurrentConstant.WAIT_TIMEOUT_M, TimeUnit.MINUTES);
    }

    /**
     * 加锁通行 Or 被阻塞
     * <p>
     * 1. tryLock 尝试加锁
     * （1）true：加锁成功：返回 true
     * （2）false：加锁失败：被阻塞，苏醒后返回 false
     * 2. waitCache 阻塞线程
     * 当线程从 waitCache 返回时，说明线程被唤醒/线程超时自动唤醒/阻塞前缓存已可用，返回 false
     *
     * @param blogId        blogId
     * @param makeSureLock  函数式接口：判断是否确认加锁
     * @param spinningBlock spinningLock
     * @return true：允许通行（加锁），false：被阻塞，并且被唤醒
     */
    private boolean allowOrWait(Long blogId, MakeSureLock makeSureLock, boolean spinningBlock) {
        if (tryLock(blogId)) {
            return true;
        }
        log.info("被 阻塞：threadList:{}，BlogId:{}", Test.thisThread(), blogId);
        waitCache(blogId, makeSureLock, spinningBlock);
        return false;
    }

    /**
     * 阻塞线程：从 waitCache 方法返回的线程，有以下三种情况：
     * （1）阻塞前，缓存已可用
     * （2）阻塞后，线程被唤醒：缓存被成功刷新
     * （3）阻塞后，超时自动苏醒：刷新缓存的线程执行异常，自动返回
     *
     * @param blogId        blogId
     * @param makeSureLock  makeSureLock
     * @param spinningBlock spinningLock
     */
    private void waitCache(long blogId, MakeSureLock makeSureLock, boolean spinningBlock) {
        if (spinningBlock) {
            int spin = this.spinTime;
            while (spin-- > 0) {
                if (!makeSureLock.sureLock()) {
                    log.info("【限流】阻塞之前，缓存已更新：自旋：{}", 4 - spin);
                    return;
                }
            }
            log.info("【限流】阻塞之前，无缓存：阻塞");
        }
        blockWaitCache(blogId);
    }

    /**
     * 尝试加锁
     *
     * @param blogId blogId
     * @return true：加锁成功    false：加锁失败
     */
    abstract boolean tryLock(Long blogId);

    /**
     * 阻塞等待缓存刷新 或 被持有锁的线程唤醒 或 超时自动苏醒
     *
     * @param blogId blogId
     */
    abstract void blockWaitCache(Long blogId);


    /**
     * 释放锁：清除锁标记，唤醒被 某篇博客阻塞的所有线程
     *
     * @param blogId blogId
     */
    abstract void unLock(Long blogId);

    @FunctionalInterface
    public interface MakeSureLock {
        /**
         * 最终确认是否加锁：防止因为网络延迟，导致 加锁线程 和 解锁线程之间的 相互错过 的情况！
         *
         * @return true：加锁 false：不加锁
         * @throws NullBlogException 用于应对 空数据 异常
         */
        boolean sureLock() throws NullBlogException;
    }

    /**
     * 监控
     */
    static class Test {
        private static AtomicInteger timeAve = new AtomicInteger(0);
        private static AtomicInteger count = new AtomicInteger(0);

        /**
         * 测试耗时
         *
         * @return 起始时间
         */
        private static long startTime() {
            return System.currentTimeMillis();
        }

        /**
         * 测试耗时
         *
         * @param time time
         */
        private static void testWastTime(long time, boolean spin, long threadId) {
            String msg = spin ? "自旋" : "阻塞";
            System.out.println(msg + "：缓存已可用（" + threadId + "）WAST:" + (time = System.currentTimeMillis() - time));
            System.out.println("平均耗时:" + timeAve.addAndGet((int) time) / count.addAndGet(1));
        }

        /**
         * @return this threadId
         */
        static long thisThread() {
            return Thread.currentThread().getId();
        }
    }
}
