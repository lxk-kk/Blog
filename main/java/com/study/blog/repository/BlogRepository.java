package com.study.blog.repository;

import com.github.pagehelper.Page;
import com.study.blog.entity.Blog;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;


/**
 * @author 10652
 * 在启动类上添加 该注解 MapperScan("com.study.blog.repository") 替代该包下的所有 mapper 注解
 */
@Repository
public interface BlogRepository {

    /**
     * 保存 blog
     *
     * @param blog 博客
     */
    void saveBlog(Blog blog);

    /**
     * 删除 blog
     *
     * @param id blog id
     */
    void removeBlog(Long id);

    /**
     * 更新博客
     *
     * @param blog 博客
     */
    void updateBlog(Blog blog);

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
     * 获取 博客创建事件
     *
     * @param id blog id
     * @return createTime
     */
    Date getCreateTime(Long id);

    /**
     * 根据用户名、博客标题 分页查询博客列表 : 最热查询？
     *
     * @param userId     用户id
     * @param title      标题
     * @param startIndex 开始页
     * @param pageSize   页面大小
     * @return 博客列表
     */
    Page<Blog> findByUserAndTitleLike(@Param("userId") int userId, @Param("title") String title, @Param
            ("pageIndex") int startIndex, @Param("pageSize") int pageSize);

    /**
     * 根据用户名、标签、标题等查询用户列表（时间逆序输出）：最新查询
     *
     * @param userId     用户id
     * @param title      标题
     * @param startIndex 开始页
     * @param pageSize   页面大小
     * @return 博客列表
     */
    Page<Blog> findByTitleLikeAndOrderByTimeDesc(@Param("userId") int userId, @Param("title") String title, @Param
            ("pageIndex") int startIndex, @Param("pageSize") int pageSize);

    /**
     * 根据分类id 获取博客列表
     *
     * @param catalogId 分类id
     * @param pageIndex 起始页
     * @param pageSize  页大小
     * @return 博客列表
     */
    Page<Blog> findByCatalog(@Param("catalogId") Long catalogId, @Param("pageIndex") int pageIndex, @Param
            ("pageSize") int pageSize);

    /**
     * 获取所有博客：应用启动时，刷新到 ES 中
     *
     * @return 博客列表
     */
    List<Blog> listAllBlog();
}
