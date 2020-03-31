package com.study.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParamVO {
    @NotNull(message = "博客id不可为null")
    Long blogId;
    @NotBlank(message = "不可提交空评论")
    String commentContent;
    /*
    * "blogId"
    * "commentContent"
    * */
}