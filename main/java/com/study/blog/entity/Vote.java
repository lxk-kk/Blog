package com.study.blog.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * @author 10652
 */
@Data
@NoArgsConstructor
public class Vote implements Serializable {
    private static final long serialVersionUID = -4387425573640287960L;
    Long id;
    @NotNull(message = "博客id不可为空")
    Long blogId;
    @NotNull(message = "用户id不可为空")
    Integer userId;
    Date createTime;

    public Vote(Long blogId, Integer userId) {
        this.blogId = blogId;
        this.userId = userId;
    }
}
