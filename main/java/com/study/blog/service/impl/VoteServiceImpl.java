package com.study.blog.service.impl;

import com.study.blog.entity.EsBlog;
import com.study.blog.entity.User;
import com.study.blog.entity.Vote;
import com.study.blog.repository.VoteRepository;
import com.study.blog.repository.es2search.EsBlogRepository;
import com.study.blog.service.BlogEvaluationCacheService;
import com.study.blog.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author 10652
 */
@Slf4j
@Service
public class VoteServiceImpl implements VoteService {

    private final EsBlogRepository esBlogRepository;
    private final BlogEvaluationCacheService cacheService;

    @Autowired
    VoteRepository voteRepository;

    @Autowired
    public VoteServiceImpl(EsBlogRepository esBlogRepository, BlogEvaluationCacheService cacheService) {
        this.esBlogRepository = esBlogRepository;
        this.cacheService = cacheService;
    }

    @Override
    public Integer getVoteUser(Long blogId, Long voteId) {
        Vote vote = cacheService.getVotedByBlogId2VoteId(blogId, voteId);
        if (Objects.isNull(vote)) {
            log.error("【取消点赞】点赞记录：为空！ ");
        }
        /*
        List<Vote> votes = cacheService.judgeVotedById(blogId)
        Integer userId = -1
        for (Vote vote : votes) {
            if (Objects.equals(vote.getId(), voteId)) {
                userId = vote.getUserId()
            }
        }
        if (userId == -1) {
            // todo
            log.error("【取消点赞】获取点赞者 id：为空！ ")
        }
        // return repository.getVoteUser(voteId)
        */
        return vote.getUserId();
    }

    @Override
    public Long isVoted(Long blogId, Integer userId) {
        // 从缓存中查询 todo
        /*List<Vote> votes = voteRepository.listVoteByBlogId(blogId);
        log.info("【list<Vote> 】:{}", votes);
        for (Vote vote : votes) {
            if (Objects.equals(vote.getUserId(), userId)) {
                return vote.getId();
            }
        }*/
        Integer isVote = voteRepository.isVoted(new Vote(blogId, userId));
        return Objects.isNull(isVote) ? 0L : isVote;
        /*Long time = System.currentTimeMillis();
        Long longv = cacheService.judgeVotedById(blogId, userId);
        System.out.println("点赞判断：1：" + (System.currentTimeMillis() - time));
        // log.info("longv:{}", longv)
        return longv;*/
    }

    @Override
    public void voteBlog(Long blogId) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assert user != null;
        Vote vote = new Vote(blogId, user.getId());
        //todo 判断一下该用户是否点过赞  这里的主键回填没啥用！
        log.info("准备点赞了！");
        // Integer isVoted = repository.isVoted(new Vote(blogId, user.getId()))
        Long isVoted = isVoted(blogId, user.getId());
        if (!Objects.isNull(isVoted) && isVoted > 0) {
            log.error("该用户已经点过赞了！");
            // 这里就不需要返回异常情况了
            return;
        }
        log.info("用户点赞了!");
        Integer voteCount = Math.toIntExact(cacheService.voteBlog(vote));
        /*
        repository.createVote(vote)
        repository.incrementVote(vote.getBlogId())
        */
        updateEsBlog(blogId, voteCount);
    }

    @Override
    public void deleteVote(Long voteId, Long blogId) {
        // 这里就不需要判断一下是否点过赞吗？
        log.info("【取消点赞】voteId：{}", voteId);
        Integer voteCount = Math.toIntExact(cacheService.cancelVotedBlog(blogId, voteId));
        /*
        repository.decrementVote(voteId)
        repository.removeVote(voteId)
        */
        updateEsBlog(blogId, voteCount);
    }

    /**
     * 修改 ES Blog (点赞量)
     *
     * @param blogId    博客id
     * @param voteCount voteCount
     */
    private void updateEsBlog(Long blogId, Integer voteCount) {
        EsBlog esBlog = null;
        try {
            esBlog = esBlogRepository.findByBlogId(blogId);
        } catch (Exception e) {
            log.error("【保存更新 ES】点赞量 ：{}", e.getMessage());
            return;
        }
        if (esBlog == null) {
            log.error("【保存更新 ES】点赞量 ：EsBlog 检索为NULL");
            return;
        }
       /*
        int plus = (judge) ? 1 : -1
        Long oldLike = esBlog.getLikeCount()

        if (oldLike == null || oldLike == 0) {
            esBlog.setLikeCount((long) 1)
        } else {
            esBlog.setLikeCount(oldLike + plus)
        }
        */
        esBlog.setLikeCount((long) voteCount);
        esBlogRepository.save(esBlog);
    }
}
