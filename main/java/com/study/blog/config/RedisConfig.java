package com.study.blog.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author 10652
 */
@Configuration
public class RedisConfig {
    @ConditionalOnMissingBean(value = RedisTemplate.class)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf, FastJsonConfig
            fastJsonConfig) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // redis 连接工厂（自动注入）
        redisTemplate.setConnectionFactory(cf);
        // 序列化方式：FastJsonRedisSerializer
        FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);
        fastJsonRedisSerializer.setFastJsonConfig(fastJsonConfig);
        // 设置 Redis 默认序列化方式 ：FastJsonRedisSerializer
        redisTemplate.setDefaultSerializer(fastJsonRedisSerializer);

        return redisTemplate;
    }

    /**
     * 以下设置：似乎默认就是这样
     *
     * @return redisConfig
     */
    @Bean
    public FastJsonConfig fastJsonConfig() {
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setSerializerFeatures(
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullNumberAsZero,
                SerializerFeature.WriteNullBooleanAsFalse,
                SerializerFeature.WriteNullListAsEmpty,
                // 不输出空值
                SerializerFeature.WriteMapNullValue,
                // 输出使用双引号
                SerializerFeature.QuoteFieldNames
        );
        return fastJsonConfig;
    }
}
