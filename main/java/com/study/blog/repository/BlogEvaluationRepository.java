package com.study.blog.repository;

import com.study.blog.dto.BlogEvaluationCacheDTO;
import com.study.blog.dto.BlogInfo;
import com.study.blog.entity.Comment;
import com.study.blog.entity.Vote;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author 10652
 */
@Repository
public interface BlogEvaluationRepository {
    /**
     * 根据 博客 id 获取 blog evaluation
     *
     * @param blogId blogId
     * @return blog evaluation
     */
    BlogEvaluationCacheDTO findByBlogId(@Param("blogId") Long blogId);

    /**
     * 根据 blogId 获取 voteList
     *
     * @param blogId blogId
     * @return voteList
     */
    List<Vote> findVoteListByBlogId(@Param("blogId") Long blogId);

    /**
     * 根据 blogId 获取 commentList
     *
     * @param blogId blogId
     * @return commentList
     */
    List<Comment> findCommentListByBlogId(@Param("blogId") Long blogId);

    /**
     * 根据 blogId 获取 blog 的 阅读量、评论量、点赞量
     *
     * @param blogId blogId
     * @return 评量信息
     */
    BlogInfo findBlogInfoByBlogId(@Param("blogId") Long blogId);

    /**
     * 批量保存 blog evaluation 信息
     *
     * @param evaluationCaches blog evaluation list
     */
    void saveBlogEvaluationByBlogId(@Param("evaluationList") List<BlogEvaluationCacheDTO> evaluationCaches);

    /**
     * 批量保存 blog evaluation
     *
     * @param readingCountMap 阅读量信息
     * @param commentMap      评论量信息
     * @param voteMap         点赞量信息
     */
    void saveBlogEvaluation(
            @Param("readCountMap") Map<Long, Long> readingCountMap,
            @Param("commentMap") Map<Long, Long> commentMap,
            @Param("voteMap") Map<Long, Long> voteMap
    );

    /**
     * 批量保存 评论列表
     *
     * @param comments comment list
     */
    void saveCommentList(@Param("commentList") List<Comment> comments);

    /**
     * 批量保存 点赞列表
     *
     * @param votes vote list
     */
    void saveVoteList(@Param("voteList") List<Vote> votes);

    /**
     * 新增 vote
     *
     * @param vote vote
     */
    void insertVote(@Param("vote") Vote vote);

    /**
     * 新增 comment
     *
     * @param comment comment
     */
    void insertComment(@Param("comment") Comment comment);

    /**
     * 删除 vote
     *
     * @param voteId voteId
     */
    void deleteVoteByVoteId(@Param("voteId") Long voteId);

    /**
     * 删除 comment
     *
     * @param commentId commentId
     */
    void deleteCommentByCommentId(@Param("commentId") Long commentId);
}
