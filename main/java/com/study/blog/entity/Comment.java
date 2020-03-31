package com.study.blog.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author 10652
 */
@Data
@NoArgsConstructor
public class Comment {
    private Long id;
    @NotNull(message = "评论者id 不可为null")
    private Integer userId;
    @NotNull(message = "评论所属博客id不可为空")
    private Long blogId;
    @NotBlank(message = "评论内容不可为空")
    private String content;
    private Date createTime;

    public Comment(Integer userId, String content, Long blogId) {
        this.blogId = blogId;
        this.userId = userId;
        this.content = content;
    }

}
