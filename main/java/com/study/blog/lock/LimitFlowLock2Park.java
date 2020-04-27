package com.study.blog.lock;

import com.study.blog.constant.ConcurrentConstant;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

/**
 * 【限流锁】
 * 1. Semaphore 实现流量限制
 * 2. 借助唯一的 blogId 作为锁标志，并以 blogId-blockedThreadList 作为 key-value 存储在 HashMap 中
 * 3. 使用 LockSupport.park(timeout) - LockSupport.unpark(thread) 实现线程的阻塞和唤醒
 * <p>
 * 如果线程请求的 blogId，在 HashMap 中不存在，则它将持有锁，主要负责如下工作：
 * （1）负责创建 blockedThreadList
 * （2）负责访问数据库，刷新缓存
 * （3）负责唤醒 blockedThreadList 中的线程（遍历 blockedThreadList 唤醒阻塞的线程）
 * <p>
 * 如果线程请求的 blogId，已经存在于 HashMap 中，则它将被阻塞，主要执行如下行为：
 * （1）将自己的 thread 实例，加入 blockedThreadList 中
 * （2）执行 LockSupport.park(timeout) 将自己阻塞
 * （3）被唤醒（超时自动苏醒）之后，查询缓存，取走数据
 *
 * @author 10652
 */
@Slf4j
public class LimitFlowLock2Park extends AbstractLimitFlowLock {

    /**
     * 保存 blogId-blockedThreadList 键值对，以 blogId 作为锁标志，以 blockedThreadList 保存阻塞的线程
     */
    private final HashMap<Long, ArrayList<Thread>> allowRequestSet;


    public LimitFlowLock2Park() {
        super();
        this.allowRequestSet = new HashMap<>(ConcurrentConstant.ALLOW_PERMISSION >> 1);
    }

    /**
     * 尝试加锁通行
     * <p>
     * 双重检测锁：检测当前线程是否可通行
     * （1）如果请求的 blogId 还未被记录，则记录当前 blogId 作为“锁标志”，并创建 blockedThreadList（给后续线程挖坑），返回 true
     * （2）双重检测锁：保证相同请求的线程中，只有一个线程加锁，其他线程被阻塞！
     * （3）缺点：锁对象为 allowRequestSet 会对所有线程起作用（不过，由于同步块内无耗时操作，相对耗时较少）
     *
     * @param blogId blogId
     * @return true：加锁成功    false：加锁失败
     */
    @Override
    boolean tryLock(Long blogId) {
        if (Objects.isNull(allowRequestSet.get(blogId))) {
            synchronized (allowRequestSet) {
                if (Objects.isNull(allowRequestSet.get(blogId))) {
                    allowRequestSet.put(blogId, new ArrayList<>());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 阻塞线程：线程加入 blockedThreadList，并将自己阻塞
     * 1. 双重检测锁：将本线程的 thread 实例，加入 blockedThreadList
     * --> 加锁：
     * （1）保证：相同的请求，并发加入 blockedArrayList 的线程安全性！
     * （2）保证：锁的释放(持有锁的线程，已将缓存刷新)与 当前线程加入blockedThreadList 能够串行化执行！
     * --> 检测 blockedThreadList 是否为 null：
     * （1）为 null：缓存已可用，直接返回，不用阻塞！
     * （2）不为 null：缓存不可用，乖乖加入 blockedThreadList！
     * --> 锁对象为 blockedThreadList，只会阻塞相同请求的 线程（同步块内，无耗时操作，相对耗时较少）
     * <p>
     * 2. 如果本线程加入了 blockedThreadList，则调用 LockSupport.parkNanos(timeout) 阻塞本线程
     * 注意：LockSupport.parkNanos 方法阻塞的线程，不会释放所持有的锁资源，因此，不能在同步块中调用！！！
     *
     * @param blogId blogId
     */
    @Override
    void blockWaitCache(Long blogId) {
        ArrayList<Thread> threadList;
        if (!Objects.isNull(threadList = allowRequestSet.get(blogId))) {
            synchronized (threadList) {
                if (!Objects.isNull(threadList = allowRequestSet.get(blogId))) {
                    // 加锁：就是为了保证 if 判断 与 threadList.add 操作的原子性
                    // 防止持有锁的线程，在当前线程判断 if 之后，清除锁标记，并在当前线程未加入 threadList 之前，便唤醒 list 中的线程
                    // 使得，当前线程加入 list，并自行阻塞之后，无人唤醒！
                    threadList.add(Thread.currentThread());
                } else {
                    return;
                }
            }
            LockSupport.parkNanos(Thread.currentThread(), ConcurrentConstant.WAIT_TIMEOUT_N);
        }
    }


    /**
     * 释放锁：清除锁标记，唤醒 blockedThreadList 中的所有线程
     * <p>
     * 注意：清除锁标记 allowRequestSet.remove(blogId) 必须加锁！
     * 1. 锁对象为 blockedThreadList
     * 2. 加锁的目的，是为了与 其他线程加入blockedThreadList 形成串行化操作！
     * （1）避免，其他线程判断 锁是否存在之后，并在将自己的 thread 加入 blockedThreadList 之前，持有锁的线程清空锁标记，唤醒list 中的线程！
     * <p>
     * （2）如果发生这种情况，那么，即将执行 list.add 将自己加入 list 的线程，将无法被唤醒，只能超时自动苏醒！
     * <p>
     * （3）在不发生指令重排的情况下， releaseLoc方法 中的 allowRequestSet.remove(blogId) 执行之后才会执行 LockSupport.unpark(thread)
     * 这能够保证，如果 set.remove 发生在 锁标志判空之前，那么线程阻塞将不会执行到 list.add，此时执行 unpark 是安全的！
     * 如果 set.remove 发生在 list.add 之后，那么，此时执行 unpark 就能将 list 中的所有线程唤醒！如果新加入的当 新加入的线程执行到 park 时，将不会被阻塞！
     * <p>
     * （4）目前不确定会不会发生指令重排，不过经过测试，目前来说，remove 与 unpark 是有序的！
     * 即：可以不将 unpark 操作放入 同步块 中！（略微提升吞吐量）
     *
     * @param blogId blogId
     */
    @Override
    void unLock(Long blogId) {
        ArrayList<Thread> threadList = allowRequestSet.get(blogId);
        if (Objects.isNull(threadList)) {
            log.error("【释放锁】：阻塞线程列表丢失！");
            // todo 抛出异常
            return;
        }
        synchronized (threadList) {
            allowRequestSet.remove(blogId);
        }
        // 暂且不将 for 循环加入上述同步块（for循环是耗时操作，同步块内能不耗时就不耗时）
        for (Thread thread : threadList) {
            LockSupport.unpark(thread);
        }
    }
}
