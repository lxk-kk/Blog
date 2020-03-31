package com.study.blog.controller;

import com.study.blog.entity.User;
import com.study.blog.service.VoteService;
import com.study.blog.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * @author 10652
 */
@RequestMapping("/vote")
@Controller
@Slf4j
public class VoteController {
    private final VoteService voteService;

    @Autowired
    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    /**
     * 点赞
     *
     * @param blogId 博客
     * @return 处理结果
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PostMapping
    public ResponseEntity<ResultVO> voteBlog(@RequestParam Long blogId) {
        log.info("博客点赞：{}", blogId);
        try {
            voteService.voteBlog(blogId);
        } catch (Throwable throwable) {
            log.error("点赞失败：错误堆栈如下：");
            throwable.printStackTrace();
            return ResponseEntity.ok().body(new ResultVO(false, throwable.getMessage()));
        }
        return ResponseEntity.ok().body(new ResultVO(true, "点赞成功"));
    }

    /**
     * 取消点赞
     *
     * @param blogId 博客id
     * @param voteId 点赞id
     * @return 处理结果
     */
    @DeleteMapping("/{blogId}/{voteId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<ResultVO> removeVote(@PathVariable("blogId") Long blogId, @PathVariable("voteId") Long
            voteId) {
        log.info("取消点赞");
        boolean isOwner = false;
        Integer userId = voteService.getVoteUser(blogId, voteId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!Objects.isNull(authentication)
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            User user = (User) authentication.getPrincipal();
            isOwner = Objects.equals(userId, user.getId());
        }
        if (!isOwner) {
            return ResponseEntity.ok().body(new ResultVO(false, "您没有权限操作"));
        }
        try {
            voteService.deleteVote(voteId, blogId);
            log.info("取消点赞成功");

        } catch (Throwable throwable) {
            throwable.printStackTrace();

            return ResponseEntity.ok().body(new ResultVO(false, throwable.getMessage()));
        }

        return ResponseEntity.ok().body(new ResultVO(true, "取消点赞成功"));
    }
}
