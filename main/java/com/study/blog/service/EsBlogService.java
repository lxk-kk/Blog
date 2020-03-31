package com.study.blog.service;

import com.study.blog.entity.EsBlog;
import com.study.blog.entity.User;
import com.study.blog.vo.TagVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @author 10652
 */
public interface EsBlogService {

    /**
     * 删除Blog
     *
     * @param id id
     */
    void removeEsBlog(String id);

    /**
     * 更新 EsBlog
     *
     * @param esBlog EsBlog
     * @return EsBlog
     */
    EsBlog updateEsBlog(EsBlog esBlog);

    /**
     * 根据 博客ID 获取博客
     *
     * @param blogId 博客id
     * @return 博客
     */
    EsBlog getEsBlogByBlogId(Long blogId);

    /**
     * 关键字模糊查询，文章按照最新排序
     *
     * @param keyword  关键字
     * @param pageable 分页
     * @return 博客列表
     */
    Page<EsBlog> listNewestEsBlog(String keyword, Pageable pageable);

    /**
     * 关键字模糊查询，文章按照最热排序
     *
     * @param keyword  关键字
     * @param pageable 分页
     * @return 博客列表
     */
    Page<EsBlog> listHotestEsBlog(String keyword, Pageable pageable);

    /**
     * 列出所有文章
     *
     * @param pageable 分页
     * @return 博客列表
     */
    Page<EsBlog> listEsBlog(Pageable pageable);

    /**
     * 最新的5篇文章
     *
     * @return 博客列表
     */
    List<EsBlog> listTop5NewestEsBlog();

    /**
     * 最热的5篇文章
     *
     * @return 博客列表
     */
    List<EsBlog> listTop5HotestEsBlog();

    /**
     * 最热的30个标签
     *
     * @return 标签列表
     */
    List<TagVO> listTop30Tag();

    /**
     * 最热的12个用户
     *
     * @return 博客列表
     */
    List<User> listTop12User();


}
