package com.study.blog.service;

import com.study.blog.entity.Catalog;

import java.util.List;

/**
 * @author 10652
 */
public interface CatalogService {
    /**
     * 保存分类
     * @param catalog 分类
     * @return 分类
     */
    Catalog saveCatalog(Catalog catalog);

    /**
     * 更新分类
     * @param catalog 分类
     * @return 分类
     */
    Catalog updateCatalog(Catalog catalog) throws Exception;

    /**
     * 删除分类
     * @param catalogId 分类id
     */
    void removeCatalog(Long catalogId);

    /**
     * 根据分类id获取分类
     * @param catalogId 分类id
     * @return 分类
     */
    Catalog getCatalogById(Long catalogId);

    /**
     * 获取分类列表
     * @param userId 博主id
     * @return 分类列表
     */
    List<Catalog> listCatalogs(Integer userId);
}
