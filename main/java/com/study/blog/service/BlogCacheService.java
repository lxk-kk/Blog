package com.study.blog.service;

import com.study.blog.entity.Blog;

/**
 * @author 10652
 */
public interface BlogCacheService {
    /**
     * 获取博客缓存
     * @param blogId 博客 id
     * @return Blog
     */
    Blog getBlogFromCacheById(long blogId);

    /**
     * 删除 博客缓存
     * @param blogId blogId
     */
    void removeBlogFromCacheById(long blogId);

    /**
     * 添加 博客缓存
     * @param blog blog
     */
    void putBlogCache(Blog blog);
}
