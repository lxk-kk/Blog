package com.study.blog.repository;

import com.study.blog.entity.Vote;
import org.apache.ibatis.annotations.Param;
import org.elasticsearch.common.recycler.Recycler;

import java.util.List;

/**
 * @author 10652
 */
public interface VoteRepository {
    /**
     * 点赞
     *
     * @param vote vote
     */
    void createVote(Vote vote);

    /**
     * 取消点赞
     *
     * @param id 点赞id
     */
    void removeVote(Long id);

    /**
     * 博客点赞量自增
     *
     * @param blogId 博客id
     */
    void incrementVote(Long blogId);

    /**
     * 博客点赞量自减
     *
     * @param voteId 博客
     */
    void decrementVote(Long voteId);

    /**
     * 判断该用户是否已经点过赞
     *
     * @param vote vote
     * @return 0/1
     */
    Integer isVoted(Vote vote);

    /**
     * 获取点赞者的id
     * @param voteId voteId
     * @return userId
     */
    int getVoteUser(Long voteId);

    /**
     * 通过 blogId 查询 vote 列表
     * @param blogId blogId
     * @return vote 列表
     */
    List<Vote> listVoteByBlogId(@Param("blogId") Long blogId);
}
