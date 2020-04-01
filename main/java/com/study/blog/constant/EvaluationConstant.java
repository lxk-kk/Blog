package com.study.blog.constant;

/**
 * @author 10652
 */
public class EvaluationConstant {
    /**
     * 缓存名称
     */
    public static final String BLOG_EVALUATION = "BLOG_EVALUATION";
    /**
     * 评论
     */
    public static final String COMMENT = "EVALUATION_COMMENT";
    /**
     * 评论量
     */
    public static final String COMMENT_COUNT = "EVALUATION_COMMENT_COUNT";
    /**
     * 点赞
     */
    public static final String VOTE = "EVALUATION_VOTE";

    /**
     * user id
     */
    public static final String USER_ID = "\"userId\"";
    /**
     * 点赞量
     */
    public static final String VOTE_COUNT = "EVALUATION_VOTE_COUNT";
    /**
     * 阅读量
     */
    public static final String READING_COUNT = "EVALUATION_READING_COUNT";

    /**
     * blog 不存在的 key
     */
    public static final String BLOG_ABSENT = "BLOG_ABSENT";

    /**
     * 缓存过期时间 30s ：针对缓存穿透问题！
     */
    public static final int EXPIRE = 30;

    /**
     * 缓存空值！
     */
    public static final int NULL = -1;

}
