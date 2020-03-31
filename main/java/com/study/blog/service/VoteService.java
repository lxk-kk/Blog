package com.study.blog.service;


/**
 * @author 10652
 */
public interface VoteService {
    /**
     * 点赞
     *
     * @param blogId 博客
     */
    void voteBlog(Long blogId);

    /**
     * 取消点赞
     *
     * @param voteId 点赞id
     * @param blogId 博客id
     */
    void deleteVote(Long voteId, Long blogId);

    /**
     * 获取点赞者的id
     *
     * @param blogId blogId
     * @param voteId voteId
     * @return userId
     */
    Integer getVoteUser(Long blogId, Long voteId);

    /**
     * 判断该用户是否已经点过赞
     *
     * @param blogId 博客id
     * @param userId 用户id
     * @return 0/1
     */
    Long isVoted(Long blogId, Integer userId);
}
