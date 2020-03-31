package com.study.blog.repository;

import com.study.blog.entity.Catalog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 10652
 */
public interface CatalogRepository {
    /**
     * 根据 博主id、分类名 查询分类列表
     *
     * @param userId      博主id
     * @param catalogName 分类名
     * @return 分类列表
     */
    List<Catalog> findByUserIdAndName(@Param("userId") int userId, @Param("name") String catalogName);

    /**
     * 根据 博主id 查询分类列表
     *
     * @param userId 博主id
     * @return 分类列表
     */
    List<Catalog> findByUserId(int userId);

    /**
     * 保存分类
     *
     * @param catalog 分类
     */
    void save(Catalog catalog);

    /**
     * 更新分类
     *
     * @param catalog 分类
     */
    void update(Catalog catalog);

    /**
     * 删除分类
     *
     * @param catalogId 分类id
     */
    void delete(Long catalogId);

    /**
     * 根据分类id查询分类
     *
     * @param catalogId 分类id
     * @return 分类
     */
    Catalog findOne(Long catalogId);

}
