package com.study.blog.service;

import com.study.blog.dto.BlogEvaluationCacheDTO;
import com.study.blog.entity.Comment;
import com.study.blog.entity.Vote;

import java.util.List;

/**
 * @author 10652
 */
public interface BlogEvaluationCacheService {
    /**
     * 根据 blogId 获取阅读量
     *
     * @param blogId blogId
     * @return 阅读量
     */
    Integer getReadingCountByBlogId(Long blogId);

    /**
     * 根据 blogId 获取 comment list
     *
     * @param blogId blogId
     * @return comment list
     */
    List<Comment> getBlogCommentListByBlogId(Long blogId);

    /**
     * 根据 blogId 获取 vote list
     *
     * @param blogId blogId
     * @param userId
     * @return vote list
     */
    Long judgeVotedById(Long blogId, Integer userId);

    /**
     * 通过 blogId voteId 获取 vote
     *
     * @param blogId blogId
     * @param voteId voteId
     * @return vote
     */
    Vote getVotedByBlogId2VoteId(Long blogId, Long voteId);

    /**
     * 从 数据库中获取 blog evaluation ：缓存中不存在，则从关系型数据库中查找
     *
     * @param blogId blogId
     * @return blog evaluation
     */
    BlogEvaluationCacheDTO getBlogEvaluationByBlogId2Mysql(Long blogId);

    /**
     * 将 数据 更新入 数据库中 ： 定时更新入关系型数据库
     */
    void saveBlogEvaluation2Mysql();

    /**
     * 从 redis 中获取 blog evaluation
     *
     * @param blogId blogId
     * @return blog evaluation blog evaluation
     */
    BlogEvaluationCacheDTO getBlogEvaluationByBlogId2Redis(Long blogId);

    /**
     * 将 数据 传入 redis 中 ： 新增
     *
     * @param blogId                 blogId
     * @param blogEvaluationCacheDTO blog evaluation
     */
    void saveBlogEvaluation2Redis(Long blogId, BlogEvaluationCacheDTO blogEvaluationCacheDTO);


    /**
     * 新增 评论 =》评论量
     *
     * @param comment comment
     * @return commentCount
     */
    Long addBlogComment(Comment comment);

    /**
     * 删除 评论 =》评论量
     *
     * @param blogId    blogId
     * @param commentId commentId
     * @return commentCount
     */
    Long deleteBlogComment(Long blogId, Long commentId);


    /**
     * 新增 点赞 =》点赞量
     *
     * @param vote vote
     * @return 点赞量
     */
    Long voteBlog(Vote vote);

    /**
     * 取消 点赞 =》点赞量
     *
     * @param blogId blogId
     * @param voteId votedId
     * @return 点赞量
     */
    Long cancelVotedBlog(Long blogId, Long voteId);

    /**
     * 访问 blog =》阅读量
     *
     * @param blogId blogId
     * @return 阅读量
     */
    Long incrementBlogReading(Long blogId);

    /**
     * 删除 缓存中的 blog evaluation记录
     *
     * @param blogId blogId
     */
    void deleteBlogEvaluation(Long blogId);
}
