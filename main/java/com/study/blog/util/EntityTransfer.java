package com.study.blog.util;

import com.github.pagehelper.Page;
import com.study.blog.entity.Comment;
import com.study.blog.entity.User;
import com.study.blog.service.UserService;
import com.study.blog.vo.CommentVO;
import com.study.blog.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author 10652
 */
@Slf4j
@Component
public class EntityTransfer {

    private static UserService userService;
    private final UserService serviceTemp;

    @Autowired
    public EntityTransfer(UserService serviceTemp) {
        this.serviceTemp = serviceTemp;
    }

    public static UserVO userToVO(User user) {
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    public static Page<UserVO> usersToVOS(Page<User> users) {
        Page<UserVO> userVOPage = new Page<>();
        BeanUtils.copyProperties(users, userVOPage);
        users.forEach(user -> userVOPage.add(EntityTransfer.userToVO(user)));
        System.out.println(userVOPage);
        return userVOPage;
    }

    public static CommentVO commentToVO(Comment comment) {
        User user = userService.searchById(comment.getUserId());
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(comment, commentVO);
        commentVO.setUser(user);
        return commentVO;
    }

    public static List<CommentVO> commentsToVOS(List<Comment> comments) {
        log.info("【comments entity convert】:{}", comments);
        if (Objects.isNull(comments) || comments.size() <= 0) {
            return new ArrayList<>(1);
        }
        return comments.stream().map(EntityTransfer::commentToVO).collect(Collectors.toList());
    }

    @PostConstruct
    void init() {
        userService = this.serviceTemp;
    }
}
