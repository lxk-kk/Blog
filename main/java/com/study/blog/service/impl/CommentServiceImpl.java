package com.study.blog.service.impl;

import com.study.blog.entity.Comment;
import com.study.blog.entity.EsBlog;
import com.study.blog.repository.EsBlogRepository;
import com.study.blog.service.BlogEvaluationCacheService;
import com.study.blog.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author 10652
 */
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final EsBlogRepository esBlogRepository;
    private final BlogEvaluationCacheService cacheService;

    @Autowired
    public CommentServiceImpl(EsBlogRepository esBlogRepository, BlogEvaluationCacheService cacheService) {
        this.esBlogRepository = esBlogRepository;
        this.cacheService = cacheService;
    }

    @Override
    public List<Comment> listComment(Long blogId) {
        // return commentRepository.listComment(blogId)
        return cacheService.getBlogCommentListByBlogId(blogId);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void publishComment(String content, Integer userId, Long blogId) {
        /*
        commentRepository.insertComment(new Comment(userId, content, blogId))
        commentRepository.incrementCommentCount(blogId)
        */
        Integer commentCount = Math.toIntExact(cacheService.addBlogComment(new Comment(userId, content, blogId)));
        updateEsBlog(blogId, commentCount);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void removeComment(Long id, Long blogId) {
        /*
        commentRepository.deleteComment(id)
        commentRepository.decrementCommentCount(blogId)
        */
        Integer commentCount = Math.toIntExact(cacheService.deleteBlogComment(blogId, id));
        updateEsBlog(blogId, commentCount);
    }

    /**
     * 更新 ES Blog
     *
     * @param blogId       博客id
     * @param commentCount commentCount
     */
    private void updateEsBlog(Long blogId, Integer commentCount) {
        EsBlog esBlog = null;
        try {
            esBlog = esBlogRepository.findByBlogId(blogId);
        } catch (Exception e) {
            log.error("【保存更新 ES】评论量 ：{}", e.getMessage());
            return;
        }
        if (esBlog == null) {
            log.error("【保存更新 ES】评论量 ：EsBlog 检索为NULL");
            return;
        }
        /*
        int plus = (judge) ? 1 : -1
        Long oldComment = esBlog.getCommentCount()

        if (oldComment == null || oldComment == 0) {
            esBlog.setCommentCount((long) 1)
        } else {
            esBlog.setCommentCount(oldComment + plus)
        }
        */
        esBlog.setCommentCount((long) commentCount);
        esBlogRepository.save(esBlog);
    }
}
