package com.study.blog.lock;

import com.study.blog.constant.ConcurrentConstant;
import com.study.blog.exception.NullBlogException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 10652
 * 多例！
 */
@Slf4j
public class LimitFlowLock {
    private HashMap<Long, ArrayList<Thread>> allowRequestSet;
    private Semaphore allowRequest;
    private int spinTime;


    public LimitFlowLock() {
        this.allowRequestSet = new HashMap<>(ConcurrentConstant.ALLOW_PERMISSION >> 1);
        this.allowRequest = new Semaphore(ConcurrentConstant.ALLOW_PERMISSION);
        spinTime = ConcurrentConstant.SPIN_TIME;
    }


    /**
     * 加锁 或者 阻塞：
     * 通过 双重检测
     *
     * @param blogId       blogId
     * @param makeSureLock 函数式接口：判断是否确认加锁
     * @return true：允许通行（加锁），false：被阻塞，并且被唤醒
     */
    private boolean allowOrWait(Long blogId, MakeSureLock makeSureLock, boolean spinningBlock) {
        // 判断是否存在相同 博客 的请求
        if (Objects.isNull(allowRequestSet.get(blogId))) {
            synchronized (this) {
                if (Objects.isNull(allowRequestSet.get(blogId))) {
                    // 当前线程 记录 博客请求，其他线程进来时，同一个 博客的 请求，就能获取该 threadList
                    allowRequestSet.put(blogId, new ArrayList<>());
                } else {
                    log.info("被 阻塞：thread:{}，BlogId:{}", Test.thisThread(), blogId);
                    waitCache(blogId, makeSureLock, spinningBlock);
                    // 提示：这是被阻塞后 被唤醒，或者是 超时自动唤醒的！
                    return false;
                }
            }
        } else {
            log.info("被 阻塞：threadList:{}，BlogId:{}", Test.thisThread(), blogId);
            // set 中已经记录了该 博客
            waitCache(blogId, makeSureLock, spinningBlock);
            // 提示：这是被阻塞后，被唤醒，或者是 超时自动唤醒的！
            return false;
        }
        return true;
    }


    /**
     * 最终加锁判断：
     * 1. 如果当前阻塞的线程数 达到 限制的数量：限流！
     * 2. 如果当前缓存已更新，则不再需要获取许可证！
     * 3. 尝试获取许可证：tryAcquire
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
     * 获取许可
     *
     * @param blogId       blogId
     * @param makeSureLock 函数式接口：判断是否确认加锁
     * @return true：加锁成功，false：线程被阻塞唤醒
     */
    public boolean limitRequestPass(Long blogId, MakeSureLock makeSureLock, boolean spinningBlock) {
        // 测试耗时：起始时间
        long time = Test.startTime();
        if (!allowOrWait(blogId, makeSureLock, spinningBlock)) {
            // 这是被唤醒的线程 / 超时自动唤醒的线程
            // 测试耗时：耗时计算
            Test.testWastTime(time, spinningBlock, Test.thisThread());
            return false;
        }
        log.info("尝试获取 Permission：thread:{}，BlogID:{}", Test.thisThread(), blogId);
        // 允许请求通行：说明这篇请求的 blog 还没有线程请求过！
        // 由于要到数据库中获取，此时，应该被限流
        try {
            if (dispatchPermission(makeSureLock)) {
                log.info("【可用 许可证】thread:{},permission：{}", Test.thisThread(), allowRequest.availablePermits());
                return true;
            }
        } catch (InterruptedException e) {
            log.error("【限流】：等待的线程（请求）被中断");
            // todo 抛出异常
        } catch (Exception e) {
            // 针对函数式接口：sureLock，防止抛出异常：导致锁未被释放！
            releasePermissionAndLock(blogId);
            throw e;
        }
        log.info("【限流】释放误加的锁：{}", Test.thisThread());
        // 释放误加的锁，并唤醒因此被阻塞的线程
        releasePermissionAndLock(blogId);
        return false;
    }

    /**
     * @param blogId blogId
     * @return 被博客阻塞的线程数
     */
    public long getSize(Long blogId) {
        ArrayList<Thread> list = allowRequestSet.get(blogId);
        return list == null ? 0 : list.size();
    }

    /**
     * 释放许可证：
     * 将许可证的释放 与 锁的释放 分离，有助于提高系统吞吐量！
     * 线程访问完数据库，就应该释放 许可证，避免因为刷新缓存的耗时，导致其他线程迟迟得不到许可证！
     */
    public void releasePermission() {
        log.info("释放 permission ：thread:{}", Test.thisThread());
        allowRequest.release();
    }

    /**
     * 释放锁，以阻塞的方式，唤醒所有等待的线程
     * <p>
     * 中断的方式唤醒线程：
     * 考虑到，将线程加入 list 的操作和阻塞线程的操作并不能保证原子性（因为，线程被阻塞，就无法处理后事：例如：释放锁）
     * ------------------------------------------------使用 wait/notifyAll ? -------------------------------------
     * 因此，如果使用 unpark 的方式唤醒线程，那么，只能唤醒 list 中已经被阻塞的线程，而即将被阻塞的线程，无法处理！
     * 而对于 运行 中的线程并不会响应中断，即使 list 中的线程还未被阻塞，那么设置了 中断标志位 之后，一旦阻塞就会中断！
     * <p>
     * 注意：blog锁的释放、线程的唤醒 必须在 synchronized 同步块中实现！
     * 原因：synchronized 具有 原子性、可见性、有序性
     * 1. 原子性：
     * blog锁的释放意味着所有的被阻塞的线程都需要被唤醒，这是一个原子性语义！
     * 2. 可见性：
     * 释放锁 就是 从 set 中移除 list，如果不能保证 list 是最新数据，那么，最新添加到 list 中的线程将不会再被唤醒，只会超时苏醒！
     * 3. 有序性：
     * 如果唤醒线程的操作 被 重排到 释放锁的逻辑之前，那么，如果唤醒线程的操作 与 释放锁的操作 执行之间，正好有其他线程加入到这个 list，那将无法被唤醒！
     *
     * @param blogId blogId
     */
    public void releaseLock(Long blogId) {
        ArrayList<Thread> threadList = allowRequestSet.get(blogId);
        if (Objects.isNull(threadList)) {
            log.error("【限流】：threadList 丢失");
            // todo 抛出异常
            return;
        }
        synchronized (threadList) {
            allowRequestSet.remove(blogId);
            for (Thread thread : threadList) {
                thread.interrupt();
            }
            threadList.clear();
        }

    }

    /**
     * 释放许可证 并 释放锁、唤醒线程
     *
     * @param blogId 博客 ID
     */
    private void releasePermissionAndLock(Long blogId) {
        releasePermission();
        releaseLock(blogId);
    }

    /**
     * 阻塞线程：将线程加入队列，并阻塞线程
     * 1. 阻塞线程前，根据使用者意愿，采取自旋的优化策略！(自旋次数可动态变化，达到性能优化的目的)
     *
     * @param blogId       blogId
     * @param makeSureLock 函数式接口：判断是否确认加锁
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
        ArrayList<Thread> threadList = allowRequestSet.get(blogId);
        if (Objects.isNull(threadList)) {
            return;
        }
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
            // todo 默认 阻塞 1 分钟：这个根据业务场景实际调整！
            LockSupport.parkNanos(Thread.currentThread(), ConcurrentConstant.WAIT_TIMEOUT_N);
        } finally {
            // 清空中断标志位!
            // 注意：LockSupport 阻塞的线程，会响应中断，但是不会抛出异常！
            Thread.interrupted();
            log.info("【限流】：等待的线程被唤醒：{}", Test.thisThread());
        }
    }

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
     * 测试
     */
    private static class Test {
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
        private static long thisThread() {
            return Thread.currentThread().getId();
        }
    }
}
