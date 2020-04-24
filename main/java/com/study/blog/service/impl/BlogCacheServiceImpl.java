package com.study.blog.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.study.blog.constant.CacheConstant;
import com.study.blog.entity.Blog;
import com.study.blog.service.BlogCacheService;
import com.study.blog.util.BlogCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author 10652
 */
@Slf4j
@Service
public class BlogCacheServiceImpl implements BlogCacheService {
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public BlogCacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void putBlogCache(Blog blog) {
        if (Objects.isNull(blog)) {
            log.error("【缓存 Blog】：blog 为 null");
            return;
        }
        String key = BlogCacheUtil.generateKey(CacheConstant.BLOG, blog.getBlogId());
        redisTemplate.opsForValue().set(key, blog, CacheConstant.BLOG_TIMEOUT, TimeUnit.HOURS);
    }

    @Override
    public Blog getBlogFromCacheById(long blogId) {
        String key = BlogCacheUtil.generateKey(CacheConstant.BLOG, blogId);
        Object blogObj = redisTemplate.opsForValue().get(key);
        if (Objects.isNull(blogObj)) {
            // log.info("【查询 Blog 缓存】：博客 无缓存");
            return null;
        }
        Blog blog;
        try {
            blog = JSONObject.parseObject(blogObj.toString(), Blog.class);
        } catch (Exception e) {
            log.error("【查询 Blog 缓存】：格式转换失败");
            return null;
        }
        // 更新过期时长
        redisTemplate.expire(key, CacheConstant.BLOG_TIMEOUT, TimeUnit.HOURS);
        return blog;
    }

    @Override
    public void removeBlogFromCacheById(long blogId) {
        String key = BlogCacheUtil.generateKey(CacheConstant.BLOG, blogId);
        redisTemplate.delete(key);
    }
}
