package com.study.blog.repository;

import com.study.blog.entity.Comment;

import java.util.List;

/**
 * @author 10652
 */
public interface CommentRepository {
    /**
     * 根据id删除评论
     *
     * @param id 评论id
     */
    void deleteComment(Long id);

    /**
     * 根据 评论id 查询评论
     *
     * @param id 评论id
     * @return 评论
     */
    Comment findCommentById(Long id);

    /**
     * 插入 评论
     *
     * @param comment 评论
     */
    void insertComment(Comment comment);

    /**
     * 获取博客评论列表
     *
     * @param blogId 评论所属的博客id
     * @return 评论列表
     */
    List<Comment> listComment(Long blogId);

    /**
     * 评论量自增（更新的博客）
     */
    void incrementCommentCount(Long blogId);

    /**
     * 评论量自减（更新的博客）
     */
    void decrementCommentCount(Long blogId);
}
