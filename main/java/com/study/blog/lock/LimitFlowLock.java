package com.study.blog.lock;

import com.study.blog.constant.ConcurrencyFlowConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 10652
 * 多例！
 */
@Slf4j
@Configuration
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LimitFlowLock {
    private HashMap<Long, ArrayList<Thread>> allowRequestSet;
    private Semaphore allowRequest;
    private AtomicInteger timeAve;
    private AtomicInteger count;


    public LimitFlowLock() {
        this.allowRequestSet = new HashMap<>(ConcurrencyFlowConstant.LIMIT_FLOW_SIZE >> 1);
        this.allowRequest = new Semaphore(ConcurrencyFlowConstant.LIMIT_FLOW_SIZE);
        this.timeAve = new AtomicInteger(0);
        this.count = new AtomicInteger(0);
    }

    /*
     · 想法：
        通过 LockSupport 阻塞线程，直到被唤醒，或者到时间自动被唤醒
        当阻塞的线程达到一定数量之后，后面到达的请求，直接返回错误页，并且，告知系统正忙，稍后重试！
     */

    /**
     * 判断是否 允许通行 或者是 阻塞
     *
     * @param blogId       blogId
     * @param makeSureLock 函数式接口：判断是否确认加锁
     * @return true：允许通行（加锁），false：被阻塞，并且被唤醒
     */
    private boolean allowOrWait(Long blogId, MakeSureLock makeSureLock, boolean spinningBlock) {
        // 判断是否存在相同 博客 的请求
        if (Objects.isNull(allowRequestSet.get(blogId))) {
            synchronized (LimitFlowLock.class) {
                if (Objects.isNull(allowRequestSet.get(blogId))) {
                    // 当前线程 记录 博客请求，其他线程进来时，同一个 博客的 请求，就能获取该 threadList
                    allowRequestSet.put(blogId, new ArrayList<>());
                } else {
                    log.info("被 阻塞：thread:{}，BlogId:{}", Thread.currentThread().getId(), blogId);
                    waitCache(blogId, makeSureLock, spinningBlock);
                    // 提示：这是被阻塞后 被唤醒，或者是 超时自动唤醒的！
                    return false;
                }
            }
        } else {
            log.info("被 阻塞：threadList:{}，BlogId:{}", Thread.currentThread().getId(), blogId);
            // set 中已经记录了该 博客
            waitCache(blogId, makeSureLock, spinningBlock);
            // 提示：这是被阻塞后，被唤醒，或者是 超时自动唤醒的！
            return false;
        }
        return true;
    }

    /**
     * 获取许可
     *
     * @param blogId       blogId
     * @param makeSureLock 函数式接口：判断是否确认加锁
     * @return true：加锁成功，false：线程被阻塞唤醒
     */
    public boolean limitRequestPass(Long blogId, MakeSureLock makeSureLock, boolean spinningBlock) {
        long time = System.currentTimeMillis();
        if (!allowOrWait(blogId, makeSureLock, spinningBlock)) {
            // 这是被唤醒的线程 / 超时自动唤醒的线程
            // log.info("限流被唤醒 | 缓存已可用 thread：{}", Thread.currentThread().getId());
            // System.out.println("缓存已可用：自旋：" + Thread.currentThread().getId() + " SUM:" + (time = System
            // .currentTimeMillis() - time));
            System.out.println("缓存已可用：唤醒：" + Thread.currentThread().getId() + " SUM:" + (time = System
                    .currentTimeMillis() - time));
            System.out.println("平均耗时:" + timeAve.addAndGet((int) time) / count.addAndGet(1));
            return false;
        }
        log.info("尝试获取 Permission：thread:{}，BlogID:{}", Thread.currentThread().getId(), blogId);
        // 允许请求通行：说明这篇请求的 blog 还没有线程请求过！
        // 由于要到数据库中获取，此时，应该被限流
        try {
            if (makeSureLock.sureLock()) {
                allowRequest.acquire();
                log.info("【剩余 permission】thread:{},{}", Thread.currentThread().getId(),
                        allowRequest.availablePermits());
                return true;
            }
        } catch (InterruptedException e) {
            log.error("【限流】：等待的线程（请求）被中断");
            // todo 抛出异常
        } catch (Exception e) {
            // 针对函数式接口：sureLock，防止抛出异常：导致锁未被释放！
            releasePermission(blogId);
            throw e;
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
    public long getSize(Long blogId) {
        ArrayList<Thread> list = allowRequestSet.get(blogId);
        return list == null ? 0 : list.size();
    }

    /**
     * 释放许可证
     *
     * @param blogId 博客 ID
     */
    public void releasePermission(Long blogId) {
        log.info("释放 permission ：thread:{}，BlogId:{}", Thread.currentThread().getId(), blogId);
        // 优先释放许可证：使得其他因为 permission 被阻塞的线程，能够即可获得 permission
        allowRequest.release();
        // 先 get threadList
        ArrayList<Thread> threadList = allowRequestSet.get(blogId);
        if (Objects.isNull(threadList)) {
            log.error("【限流】：threadList 丢失");
            // todo 抛出异常
            return;
        }
        // 保证释放 threadList 以及 唤醒 list 中的所有线程的 行为，一定在 添加其他线程的行为 之前 或者 之后！
        // 如果在之前：那么，这里就能通过 LockSupport 唤醒新添加是线程
        // 如果在这之后，那么，set 中一定不存在相关的 threadList
        // 删除 set 中的 threadList
        synchronized (threadList) {
            allowRequestSet.remove(blogId);
            // 通过已经 get 到的 threadList 唤醒 list 中的所有线程
            for (Thread thread : threadList) {
                thread.interrupt();
                // 以中断的方式，唤醒线程！
                // 对于 已经添加到 threadList 中，但是还未被 park 的线程，在 park 之前，检测中断标志即可！
                // LockSupport.unpark(thread);
            }
            threadList.clear();
        }
    }

    /**
     * 阻塞线程
     *
     * @param blogId       blogId
     * @param makeSureLock 函数式接口：判断是否确认加锁
     */
    private void waitCache(long blogId, MakeSureLock makeSureLock, boolean spinningBlock) {
        if (spinningBlock) {
            // 优化策略：阻塞之前先自旋：检查缓存是否立即就被更新！
            int spin = 4;
            while (spin-- > 0) {
                if (!makeSureLock.sureLock()) {
                    log.info("【限流】阻塞之前，缓存已更新：自旋：{}", 4 - spin);
                    return;
                }
            }
            log.info("【限流】阻塞之前，无缓存：阻塞");
        }
        ArrayList<Thread> threadList = allowRequestSet.get(blogId);
        if (Objects.isNull(threadList)) {
            // 该 list 已经被释放：缓存已经刷新，当前线程不用被阻塞！
            return;
        }
        // 首先获取 threadList
        boolean lockJudge = false;

        // 加锁：使得 唤醒该list中的 thread 的行为，要么，在这之前完成，要么在这之后完成！
        // 1. 如果在这之前完成，那么 set 中的 list 一定为 null
        // 2. 在这之后完成，那么 当前线程就能够被其他线程成功唤醒！
        synchronized (threadList) {
            if (!Objects.isNull(threadList = allowRequestSet.get(blogId))) {
                // 将当前线程 加入 List！
                threadList.add(Thread.currentThread());
                lockJudge = true;
            }
        }
        if (!lockJudge) {
            // 说明 set 中的 threadList == null：缓存以刷新，直接返回！
            return;
        }
        try {
            // 阻塞线程之前，先判断线程是否已经被中断！
            if (Thread.interrupted()) {
                return;
            }
            // todo 默认 阻塞 5 分钟：这个根据业务场景实际调整！
            LockSupport.parkNanos(Thread.currentThread(), ConcurrencyFlowConstant.WAIT_TIMEOUT);
        } finally {
            // 清空中断标志位!
            // 注意：LockSupport 阻塞的线程，会响应中断，但是不会抛出异常！
            Thread.interrupted();
            log.info("【限流】：等待的线程被唤醒：{}", Thread.currentThread().getId());
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
