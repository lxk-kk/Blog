package com.study.blog.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author 10652
 */
@Slf4j
public class BlogCacheUtil {
    public static String generateKey(Long id1, Long id2) {
        if (Objects.isNull(id1) || Objects.isNull(id2)) {
            // todo id为null 抛出异常
            log.error("【 生成 key 】 id 为 null ！");
        }
        return String.valueOf(id1) + ":" + id2;
    }

    public static String generateKey(String key, Long id2) {
        if (Objects.isNull(key) || Objects.isNull(id2)) {
            // todo id为null 抛出异常
            log.error("【 生成 key 】 id 为 null ！");
        }
        return String.valueOf(key) + ":" + id2;
    }

    public static String generateKey(String key, Integer id2) {
        if (Objects.isNull(key) || Objects.isNull(id2)) {
            // todo id为null 抛出异常
            log.error("【 生成 key 】 id 为 null ！");
        }
        return String.valueOf(key) + ":" + id2;
    }
}
