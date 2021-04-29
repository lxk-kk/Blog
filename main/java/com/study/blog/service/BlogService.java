package com.study.blog.service;

import com.github.pagehelper.Page;
import com.study.blog.entity.Blog;
import com.study.blog.entity.User;

import java.util.List;

/**
 * @author 10652
 */
public interface BlogService {
    /**
     * 保存 blog
     *
     * @param blog 博客
     * @param user user
     * @return blog
     */
    Blog saveBlog(Blog blog, User user);

    /**
     * 删除 blog
     *
     * @param id blog id
     */
    void removeBlog(Long id);

    /**
     * 更新blog
     *
     * @param blog blog
     * @param user user
     * @return blog
     */
    Blog updateBlog(Blog blog, User user);

    /**
     * 根据 博客 id获取博客
     *
     * @param id 博客id
     * @return 博客
     */
    Blog getBlogById(Long id);

    /**
     * 阅读量自增
     *
     * @param id 博客id
     */
    void readingIncrement(Long id);

    /**
     * 根据分类查询博客
     *
     * @param catalogId 分类id
     * @param pageNum   起始页
     * @param pageSize  页大小
     * @return blogs
     */
    Page<Blog> listBlogByCatalog(Long catalogId, int pageNum, int pageSize);

    /**
     * 根据 用户 标题 模糊查询博客
     *
     * @param userId    用户id
     * @param title     标题
     * @param startPage 起始页
     * @param pageSize  页大小
     * @return 博客列表
     */
    Page<Blog> listBlogsByTitleLike(int userId, String title, int startPage, int pageSize);

    /**
     * 根据 用户 标题 模糊查询博客，并根据时间逆序排序（从小到大）：最新博客查询
     *
     * @param userId    用户id
     * @param title     博客标题
     * @param startPage 起始页
     * @param pageSize  页大小
     * @return 博客列表
     */
    Page<Blog> listBlogsByTitleLikeAndDescSort(int userId, String title, int startPage, int pageSize);

    /**
     * @return 博客列表
     */
    List<Blog> listAllBlog();
}
