package com.study.blog.repository.es2search;

import com.study.blog.entity.EsBlog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Blog 储存库
 *
 * @author 10652
 */
public interface EsBlogRepository extends ElasticsearchRepository<EsBlog, String> {
    /**
     * 模糊去重查询
     * @param title 标题
     * @param summary 总结
     * @param content 内容
     * @param tags 标签
     * @param pageable 分页
     * @return EsBlog
     */
    Page<EsBlog> findDistinctByTitleContainingOrSummaryContainingOrContentContainingOrTagsContaining(String title,
                                                                                                     String summary,
                                                                                                     String content,
                                                                                                     String tags,
                                                                                                     Pageable pageable);

    /**
     * 根据 博客id 查询
     *
     * @param blogId 博客 id
     * @return 博客
     */
    EsBlog findByBlogId(Long blogId);
}
