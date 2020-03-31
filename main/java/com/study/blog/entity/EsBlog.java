package com.study.blog.entity;

import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * @author 10652
 * <p>
 * XmlRootElement ：将 MediaType 转换为 XML
 */
@Document(indexName = "blog", type = "blog")
@XmlRootElement
@Data
public class EsBlog implements Serializable {
    private static final long serialVersionUID = 196033427711217910L;

    /**
     * 主键
     */
    @Id
    private String id;

    /**
     * 博客id：不做分词：不做全文检索字段
     */
    @Field(type = FieldType.Long)
    private Long blogId;

    /**
     * 标题
     */
    private String title;

    /**
     * 总结
     */
    private String summary;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签
     */
    @Field(fielddata = true, type = FieldType.Text)
    private String tags;

    /**
     * 用户账号
     */
    @Field(index = false, type = FieldType.Keyword)
    private String username;

    /**
     * 头像
     */
    @Field(index = false, type = FieldType.Text)
    private String avatar;

    /**
     * 阅读量
     */
    @Field(index = false, type = FieldType.Long)
    private Long readCount;
    /**
     * 评论量
     */
    @Field(index = false, type = FieldType.Long)
    private Long commentCount;
    /**
     * 点赞量
     */
    @Field(index = false, type = FieldType.Long)
    private Long likeCount;

    /**
     * 创建时间
     */
    @Field(index = false, type = FieldType.Date)
    private Date createTime;

    /**
     * JPA 的规范：要求无参构造器：设置 protected 防止直接使用
     */
    protected EsBlog() {
    }

    public EsBlog(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public EsBlog(Long blogId, String title, String summary, String content, String tags, String username, String
            avatar, Long readSize, Long commentSize, Long voteSize, Date createTime) {
        this.blogId = blogId;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.tags = tags;
        this.username = username;
        this.avatar = avatar;
        this.readCount = readSize;
        this.commentCount = commentSize;
        this.likeCount = voteSize;
        this.createTime = createTime;
    }

    public EsBlog(Blog blog, User user) {
        BeanUtils.copyProperties(blog, this);
        this.avatar = user.getAvatar();
        this.username = user.getUsername();
    }

    public void update(Blog blog, User user) {
        /*
        注意：这个blog是直接从前端拿回来的！有数据不全！由于修改博客只需要涉及以下内容，所以单独赋值！
        title、summary、content、tags、catalog（这里 ES Blog 中不涉及 catalog！）
        BeanUtils.copyProperties(blog, this)
         */
        this.title = blog.getTitle();
        this.summary = blog.getSummary();
        this.content = blog.getContent();
        this.tags = blog.getTags();
        this.username = user.getUsername();
        this.avatar = user.getAvatar();
        // todo ：没必要
        this.createTime = blog.getCreateTime();
    }
}


























