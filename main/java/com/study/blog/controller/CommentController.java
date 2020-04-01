package com.study.blog.controller;

import com.study.blog.annotation.ValidateAnnotation;
import com.study.blog.entity.Comment;
import com.study.blog.entity.ID;
import com.study.blog.entity.User;
import com.study.blog.service.CommentService;
import com.study.blog.service.IDService;
import com.study.blog.util.EntityTransfer;
import com.study.blog.vo.ParamVO;
import com.study.blog.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 10652
 */
@Controller
@Slf4j
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;
    private final IDService idService;

    @Autowired
    public CommentController(CommentService commentService, IDService idService) {
        this.commentService = commentService;
        this.idService = idService;
    }

    /**
     * 获取评论列表：判断是否为评论所有者的逻辑似乎有点问题
     *
     * @param blogId 评论id
     * @param model  model
     * @return url
     */
    @GetMapping
    public String listComment(@RequestParam("blogId") Long blogId, Model model) {
        log.info("评论列表！");
        // 缓存中获取：如果不存在 会自动从数据库中获取
        List<Comment> comments = commentService.listComment(blogId);
        log.info("comments:{}", comments);
        /*
        * 判断操作用户是否为评论的所有者
        * */
        String commentOwner = "";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            User principal = (User) authentication.getPrincipal();
            if (principal != null) {
                commentOwner = principal.getUsername();
            }
        }
        model.addAttribute("commentOwner", commentOwner);
        model.addAttribute("comments", EntityTransfer.commentsToVOS(comments));
        return "/userspace/blog::#mainContainerRepleace";
    }

    /**
     * 发表评论：指定的角色才能发表评论
     *
     * @return 处理结果
     */
    @PostMapping
    @ResponseBody
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @ValidateAnnotation(authorityId = 2)
    public ResponseEntity<ResultVO> publishComment(@RequestBody @Validated ParamVO paramVO, BindingResult
            bindingResult) {
        if (bindingResult.hasErrors()) {
            // 接口验证
            log.error("字段验证：{}",bindingResult.getFieldError().getDefaultMessage());
            return ResponseEntity.ok().body(new ResultVO(false, bindingResult.getFieldError().getDefaultMessage()));
        }
        Long blogId = paramVO.getBlogId();
        String commentContent = paramVO.getCommentContent();
        log.info("发表评论！");
        Integer userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            User user = (User) authentication.getPrincipal();
            userId = user.getId();
        }
        // 抛出异常：如果 userId == null
        try {
            commentService.publishComment(commentContent, userId, blogId);
        } catch (Throwable throwable) {
            return ResponseEntity.ok().body(new ResultVO(false, throwable.getMessage()));
        }
        return ResponseEntity.ok().body(new ResultVO(true, "处理成功", null));
    }

    /**
     * 删除评论：通过评论id删除评论
     *
     * @param id     评论id
     * @param blogId 所属博客id
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    @ValidateAnnotation(authorityId = 2)
    public ResponseEntity<ResultVO> deleteComment(@PathVariable("id") Long id, Long blogId) {
        log.info("删除评论！");
        // 判断操作用户是否是博客/评论的所有者
        boolean isOwner = false;
        ID userId = idService.getUserId(blogId, id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            User user = (User) authentication.getPrincipal();
            isOwner = user != null
                    && (user.getId().equals(userId.getBlogUserId()) || user.getId().equals(userId.getCommentUserId()));
        }
        //如果不是则返回false：提示无权限操作
        if (!isOwner) {
            return ResponseEntity.ok().body(new ResultVO(false, "您无权限操作"));
        }
        //否则，移除评论，返回true：提示操作成功
        try {
            commentService.removeComment(id, blogId);
        } catch (Throwable throwable) {
            return ResponseEntity.ok().body(new ResultVO(false, throwable.getMessage()));
        }
        return ResponseEntity.ok().body(new ResultVO(true, "操作成功"));
    }
}
