package com.study.blog.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * redis 分布式锁
 *
 * @author 10652
 */
@Slf4j
@Component
@Deprecated
public class RedisDistributedLock {
    /**
     * 过期时长（s）：30 s
     */
    private static final Long EXPIRE_TIME = 2L;
    /**
     * 成功解锁标识
     */
    private static final Long UNLOCK_SUCCESSFUL = 1L;

    private static RedisTemplate<String, Object> redisTemplateTemp;
    private final RedisTemplate<String, Object> redisTemplate;


    @Autowired
    public RedisDistributedLock(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 加锁
     *
     * @param blogId    blogId
     * @param requestId requestId
     * @return true/false
     */
    public static boolean lock(String blogId, String requestId) {
        /*
            设置 key-value-expire
            1、设置成功：返回 true：加锁成功：同时设置了过期时间（原子操作）
            2、设置失败：返回 false：加锁失败
         */
        Boolean judge = redisTemplateTemp.opsForValue().setIfAbsent(blogId, requestId, EXPIRE_TIME, TimeUnit.SECONDS);
        /*
            因为返回的是 boolean 基本类型：所以需要防止 NPE 问题！
         */
        judge = (judge == null) ? false : judge;
        log.info("【加锁】{}:{}", judge, Thread.currentThread());
        return judge;
    }

    /**
     * 解锁
     *
     * @param blogId    blogId
     * @param requestId requestId
     * @return true/false
     */
    public static boolean unlock(String blogId, String requestId) {
        /*
            lua 脚本：Redis使用单个 Lua 解释器去运行所有脚本，并且Redis也保证脚本会以原子性的方式执行：当某个脚本正在运行的时候，不会有其他脚本或者Redis命令被执行
         */
        String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        Long result = null;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        // 以文本的形式写入脚本
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        // 以文件的形式写入脚本
        //redisScript.setLocation(new ClassPathResource("lua/unlock.lua"))
        try {
            result = redisTemplateTemp.execute(redisScript, Collections.singletonList(blogId), requestId);
            log.info("【解锁】{}:{}", result, Thread.currentThread());
        } catch (Exception e) {
            log.error("【解锁】失败：{}:{}", Thread.currentThread(), e.getMessage());
        }
        return Objects.equals(result, UNLOCK_SUCCESSFUL);
    }

    @PostConstruct
    void init() {
        redisTemplateTemp = redisTemplate;
    }
}
