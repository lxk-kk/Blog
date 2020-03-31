package com.study.blog.service;

import com.study.blog.entity.Comment;

import java.util.List;

/**
 * @author 10652
 */
public interface CommentService {
    /**
     * 列出博客的所有评论
     *
     * @param blogId 博客id
     * @return 评论列表
     */
    List<Comment> listComment(Long blogId);

    /**
     * 发布评论
     *
     * @param content 评论内容
     * @param userId  发布者id
     * @param blogId  所属博客id
     */
    void publishComment(String content, Integer userId, Long blogId);

    /**
     * 根据 评论id 删除评论
     *
     * @param id     评论id
     * @param blogId 博客id
     */
    void removeComment(Long id, Long blogId);
}
