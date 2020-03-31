package com.study.blog.entity;

import com.github.rjeschke.txtmark.Processor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author 10652
 */
@Slf4j
@Setter
@Getter
@NoArgsConstructor
@ToString
public class Blog {
    private Long blogId;
    private Integer userId;
    @NotBlank(message = "请填写标题")
    @Size(min = 2, max = 64)
    private String title;
    @NotBlank(message = "请填写摘要")
    @Size(min = 2, max = 512)
    private String summary;
    @NotBlank(message = "请填写内容")
    private String content;
    @NotBlank(message = "请填写内容")
    private String htmlContent;
    /**
     * 阅读量
     */
    private Long readCount;
    /**
     * 评论量
     */
    private Long commentCount;
    /**
     * 点赞量
     */
    private Long likeCount;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 标签：由命名可知，虽然这里是字符串，但是是由多个标签组合而成，不同标签之间通过英文逗号分隔（前端插件组合）
     */
    private String tags;

    /**
     * 博客分类
     */
    @NotNull(message = "请选择分类")
    private Catalog catalog;

    /**
     * 博客评论列表
     */
    private List<Comment> comments;

    public Blog(String title, String summary, String content) {
        this.title = title;
        this.summary = summary;
        setContent(content);
        this.readCount = 0L;
        this.commentCount = 0L;
        this.likeCount = 0L;
    }

    /**
     * 新增评论
     *
     * @param commentContent 评论内容
     * @param userId         评论者id
     */
    public void addComment(String commentContent, Integer userId) {
        Comment comment = new Comment(userId, commentContent, this.blogId);
        this.comments.add(comment);
        this.commentCount = (long) this.comments.size();
    }

    /**
     * 删除评论
     *
     * @param commentId 评论id
     */
    public void removeComment(Long commentId) {
        for (int i = 0; i < commentCount; i++) {
            if (Objects.equals(this.comments.get(i).getBlogId(), commentId)) {
                this.comments.remove(i);
                this.commentCount = (long) this.comments.size();
                return;
            }
        }
    }

    /**
     * 设置content：包括 md content 以及 html content
     * 将 md content 转换为 html content
     *
     * @param content md content
     */
    public void setContent(String content) {
        if (content == null) {
            this.htmlContent = "";
            this.content = "";
            return;
        }
        this.content = content;
        /*
        * md 解析器：将 md 内容格式转换为 html 内容格式
        * */
        this.htmlContent = Processor.process(content);
    }
}
