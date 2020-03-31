package com.study.blog.vo;

import com.study.blog.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * @author 10652
 */
@Data
@NoArgsConstructor
public class CommentVO implements Serializable {
    private static final long serialVersionUID = -190118626718393189L;
    private Long id;
    private User user;
    @NotNull(message = "评论所属博客id不可为空")
    private Long blogId;
    @NotBlank(message = "评论内容不可为空")
    private String content;
    private Date createTime;
}
