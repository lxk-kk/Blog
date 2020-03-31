package com.study.blog.dto;

import com.study.blog.entity.Comment;
import com.study.blog.entity.Vote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 每篇博客的评量：评论、点赞、阅读
 *
 * @author 10652
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogEvaluationCacheDTO implements Serializable {
    private static final long serialVersionUID = 589749372520909459L;
    Long blogId;
    List<Comment> comments;
    Integer commentCount;
    List<Vote> votes;
    Integer voteCount;
    Integer readingCount;
}
