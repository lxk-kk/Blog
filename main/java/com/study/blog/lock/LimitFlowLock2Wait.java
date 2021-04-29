package com.study.blog.lock;

import com.study.blog.constant.ConcurrentConstant;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Objects;

/**
 * 【限流锁】
 * 1. Semaphore 实现流量限制
 * 2. 借助唯一的 blogId 作为锁标志，并以 blogId-lockObject 作为 key-value 存储在 HashMap 中
 * 3. 使用 lockObject.wait(timeout) - lockObject.notifyAll() 实现线程的阻塞和唤醒
 * <p>
 * 如果线程请求的 blogId，在 HashMap 中不存在，则它将持有锁，主要负责如下工作：
 * （1）负责创建锁对象（将线程自身的 thread 实例作为锁对象）,阻塞后续线程（lockObject.wait(timeout)）
 * （2）负责访问数据库，刷新缓存
 * （3）负责通过锁对象，唤醒被锁对象阻塞的所有线程( lockObject.notifyAll() )
 * <p>
 * 如果线程请求的 blogId，已经存在于 HashMap 中，则它将被阻塞，主要执行如下行为：
 * （1）从 HashMap 中取出锁对象（lockObject），执行 lockObject.wait(timeout) 阻塞自己
 * （2）被唤醒（超时自动苏醒）之后，查询缓存，取走数据
 *
 * @author 10652
 */
@Slf4j
public class LimitFlowLock2Wait extends AbstractLimitFlowLock {
    /**
     * 保存 blogId-lockObject 键值对：
     * --> 以 blogId 作为锁标志
     * --> 以持有锁的线程的 thread 实例作为锁对象（lockObject），阻塞后续所有线程
     */
    private final HashMap<Long, Thread> allowRequestSet;


    public LimitFlowLock2Wait() {
        this.allowRequestSet = new HashMap<>(ConcurrentConstant.ALLOW_PERMISSION >> 1);
    }

    /**
     * 尝试加锁通行
     * <p>
     * 1. 双重检测锁：检测当前线程是否可通行
     * （1）如果请求的 blogId 还未被记录，则记录当前 blogId 作为“锁标志”，并将当前线程的 thread 实例作为锁对象（lockObject），返回 true
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
                    allowRequestSet.put(blogId, Thread.currentThread());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 阻塞线程：线程通过锁对象 lockObject.wait(timeout) 将自己阻塞
     * <p>
     * 1. 双重检测锁：取出锁对象，执行 lockObject.wait(timeout) 阻塞当前线程
     * --> 加锁：
     * （1）wait - notify 等待通知机制依赖于 对象监视器
     * （2）必须保证锁标志的判断 与 线程的阻塞是一个原子性操作：防止判断锁标志之后，阻塞线程之前，锁标志被清除！
     * --> 检测 lockObject 是否为 null：
     * （1）为 null：缓存已可用，直接返回，不用阻塞！
     * （2）不为 null：缓存不可用，乖乖执行 lockObject.wait(timeout) 安静的等待一会儿！
     * --> synchronized 锁对象为 lockObject：
     * （1）清除blogId锁标志 与 本线程的阻塞 串行化执行！
     *
     * @param blogId blogId
     */
    @Override
    void blockWaitCache(Long blogId) {
        Thread lockObject;
        if (!Objects.isNull(lockObject = allowRequestSet.get(blogId))) {
            synchronized (lockObject) {
                if (!Objects.isNull(lockObject = allowRequestSet.get(blogId))) {
                    try {
                        lockObject.wait(ConcurrentConstant.WAIT_TIMEOUT_MILLS);
                    } catch (InterruptedException e) {
                        // todo 阻塞的线程被中断 --- 重置中断标志？？要不要重置？
                        // Thread.interrupted();
                        log.info("【阻塞】等待缓存刷新的线程 {} 被中断", Thread.currentThread());
                    }
                }
            }
        }
    }

    /**
     * 释放锁
     * 1. 检测当前线程是否有资格释放锁！
     * 如果当前线程 与 加锁的线程 并不是同一个，将拒绝执行释放锁的操作！（确保安全性）
     * <p>
     * 2. 在 synchronized同步块中 清除锁标记，唤醒所有被阻塞的线程
     * （1）加锁：lockObject.notifyAll 依赖于对象监视器！
     * （2）synchronized 作用对象：lockObject：这是为了和阻塞线程的操作互斥执行！
     * （3）清除锁标记
     * 并不需要和加锁逻辑互斥，除非，刷新缓存是异步执行的！
     * 清除锁标记 和 唤醒线程 必须是原子性操作：避免当前线程唤醒 lockObject 上阻塞的线程之后，其他线程又通过 lockObject 阻塞自己
     *
     * @param blogId blogId
     */
    @Override
    void unLock(Long blogId) {
        Thread lockObject = allowRequestSet.get(blogId);
        if (Objects.isNull(lockObject)) {
            log.error("【释放锁】：锁丢失");
            // todo 抛出异常
            return;
        }
        if (!Objects.equals(lockObject, Thread.currentThread())) {
            log.error("【释放锁】无权限：加锁 & 释放锁：不是同一个线程");
            return;
        }
        synchronized (lockObject) {
            allowRequestSet.remove(blogId);
            lockObject.notifyAll();
        }
    }
}
/*
有一种情况：清除锁标记之后，其他线程立马就有开始加锁了！
正常情况下：释放锁必须在 刷新缓存 之后执行！
如果刷新缓存是异步执行的，那么，由于 清除锁标记 与 加锁 的逻辑并不是互斥的，因此很可能在释放锁之后，缓存还未刷新，导致后续线程不断尝试获取锁！
 */