package com.study.blog.util;

import com.study.blog.constant.ConcurrencyFlowConstant;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 10652
 */
@Slf4j
public class LimitFlowLock {
    private static ConcurrentHashMap<Long, ArrayList<Thread>> allowRequestSet = new ConcurrentHashMap<>(
            ConcurrencyFlowConstant.LIMIT_FLOW_SIZE >> 1
    );
    private static Semaphore allowRequest = new Semaphore(ConcurrencyFlowConstant.LIMIT_FLOW_SIZE);

    private static void waitCache(long blogId) {
        // 相同博客 的请求，其他线程到这里来！
        ArrayList<Thread> threadList = allowRequestSet.get(blogId);
        if (Objects.isNull(threadList)) {
            // 说明刷新缓存的线程执行的太快，已经释放通信证了！
            log.info("缓存已被刷新：{}", Thread.currentThread().getId());
            return;
        }
        // 缓存还未刷新完成，被阻塞！
        try {
            // 将当前线程加入 List
            threadList.add(Thread.currentThread());
            // log.info("【限流】阻塞线程数量：{}", allowRequestSet.get(blogId).size());
            // todo 默认 阻塞 5 分钟：这个根据业务场景实际调整！
            LockSupport.parkNanos(Thread.currentThread(), ConcurrencyFlowConstant.WAIT_TIMEOUT);
        } catch (Exception e) {
            log.error("【限流】：等待的线程（请求）被中断");
            // todo 抛出异常
        }
    }
    /*
    想法：
        通过 ReentrantLock 阻塞线程，直到被唤醒，或者到时间自动被唤醒
        当阻塞的线程达到一定数量之后，后面到达的请求，直接返回错误页，并且，告知系统正忙，稍后重试！
     */

    private static boolean allowOrWait(Long blogId) {
        // 判断是否存在相同 博客 的请求
        if (Objects.isNull(allowRequestSet.get(blogId))) {
            synchronized (LimitFlowLock.class) {
                if (Objects.isNull(allowRequestSet.get(blogId))) {
                    // 当前线程 记录 博客请求，其他线程进来时，同一个 博客的 请求，就能获取该 Condition 对象
                    allowRequestSet.put(blogId, new ArrayList<>());
                } else {
                    log.info("被 阻塞：threadList:{}，BlogId:{}", Thread.currentThread().getId(), blogId);
                    waitCache(blogId);
                    // 提示：这是被阻塞后 被唤醒，或者是 超时自动唤醒的！
                    return false;
                }
            }
        } else {
            log.info("被 阻塞：threadList:{}，BlogId:{}", Thread.currentThread().getId(), blogId);
            // set 中已经记录了该 博客
            waitCache(blogId);
            // 提示：这是被阻塞后，被唤醒，或者是 超时自动唤醒的！
            return false;
        }
        return true;
    }

    public static boolean limitRequestPass(Long blogId, MakeSureLock makeSureLock) {
        if (!allowOrWait(blogId)) {
            // 这是被唤醒的线程 / 超时自动唤醒的线程
            log.info("限流被唤醒 | 缓存已可用 thread：{}", Thread.currentThread().getId());
            return false;
        }
        log.info("获得通行证：thread:{}，BlogID:{}", Thread.currentThread().getId(), blogId);
        // 允许请求通行：说明这篇请求的 blog 还没有线程请求过！
        // 由于要到数据库中获取，此时，应该被限流
        if (makeSureLock.sureLock()) {
            try {
                allowRequest.acquire();
            } catch (InterruptedException e) {
                log.error("【限流】：等待的线程（请求）被中断");
                // todo 抛出异常
            }
            return true;
        }
        log.info("【限流】释放误加的锁：{}", Thread.currentThread().getId());
        // 释放误加的锁，并唤醒因此被阻塞的线程
        releasePermission(blogId);
        return false;
    }

    /**
     * 测试 被阻塞的线程数
     *
     * @param blogId blogId
     * @return size
     */
    public static long getSize(Long blogId) {
        List list = allowRequestSet.get(blogId);
        return list == null ? 0 : list.size();
    }

    /**
     * 释放许可证
     *
     * @param blogId 博客 ID
     */
    public static void releasePermission(Long blogId) {
        log.info("释放通行证：thread:{}，BlogId:{}", Thread.currentThread().getId(), blogId);
        // 最开始释放许可证：让其他被阻塞的线程能够少阻塞一会儿
        // 由于能够保证 某篇博客 的单线程操作
        // 因此，这里不会出现并发被修改同一个 博客 的现象
        ArrayList<Thread> threadList = allowRequestSet.get(blogId);
        // 清空 请求该博客的锁记录
        allowRequestSet.remove(blogId);
        if (Objects.isNull(threadList)) {
            log.error("【限流】：threadList 丢失");
            // todo 抛出异常
            return;
        }
        // 唤醒所有线程
        for (Thread thread : threadList) {
            LockSupport.unpark(thread);
        }
    }

    @FunctionalInterface
    public interface MakeSureLock {
        /**
         * 最终确认是否加锁：防止因为网络延迟，导致 加锁线程 和 解锁线程之间的 相互错过 的情况！
         *
         * @return true：加锁 false：不加锁
         */
        boolean sureLock();
    }
}
