package com.study.blog.service.impl;

import com.study.blog.entity.Catalog;
import com.study.blog.repository.CatalogRepository;
import com.study.blog.service.CatalogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 10652
 */
@Service
@Slf4j
public class CatalogServiceImpl implements CatalogService {
    private final CatalogRepository repository;

    @Autowired
    public CatalogServiceImpl(CatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Catalog saveCatalog(Catalog catalog) {
        List<Catalog> catalogs = repository.findByUserIdAndName(catalog.getUserId(), catalog.getName());
        if (catalogs != null && catalogs.size() > 0) {
            log.error("【分类管理】保存分类：该分类已经存在");
            // todo 该分类已经存在：抛出异常
            throw new IllegalArgumentException("该分类已经存在");
        }
        repository.save(catalog);
        log.info("【保存分类】 catalog:{}", catalog);
        return catalog;
    }

    @Override
    public Catalog updateCatalog(Catalog catalog) {
        try {
            repository.update(catalog);
        } catch (Exception e) {
            log.error("【分类管理】更新分类：更新异常");
            throw new IllegalArgumentException(e.getMessage());
        }
        return catalog;
    }

    @Override
    public void removeCatalog(Long catalogId) {
        repository.delete(catalogId);
    }

    @Override
    public Catalog getCatalogById(Long catalogId) {
        return repository.findOne(catalogId);
    }

    @Override
    public List<Catalog> listCatalogs(Integer userId) {
        return repository.findByUserId(userId);
    }
}
