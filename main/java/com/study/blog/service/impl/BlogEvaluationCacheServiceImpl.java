package com.study.blog.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.study.blog.constant.CacheConstant;
import com.study.blog.constant.ConcurrencyFlowConstant;
import com.study.blog.constant.ValidateConstant;
import com.study.blog.dto.BlogEvaluationCacheDTO;
import com.study.blog.entity.Comment;
import com.study.blog.entity.Vote;
import com.study.blog.exception.LimitFlowException;
import com.study.blog.exception.NullBlogException;
import com.study.blog.lock.LimitFlowLock;
import com.study.blog.repository.BlogEvaluationRepository;
import com.study.blog.service.BlogEvaluationCacheService;
import com.study.blog.util.BlogCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 10652
 */
@Slf4j
@Service
public class BlogEvaluationCacheServiceImpl implements BlogEvaluationCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final BlogEvaluationRepository blogEvaluationRepository;
    private final LimitFlowLock limitFlowLock;


    @Autowired
    public BlogEvaluationCacheServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                          BlogEvaluationRepository blogEvaluationRepository) {
        this.redisTemplate = redisTemplate;
        this.blogEvaluationRepository = blogEvaluationRepository;
        this.limitFlowLock = getLimitFlowLock();
    }

    @Lookup
    private LimitFlowLock getLimitFlowLock() {
        LimitFlowLock limitFlowLock = new LimitFlowLock();
        log.info("【Lock_EVALUATION】:{}", limitFlowLock);
        return limitFlowLock;
    }

    /**
     * 获取 阅读量
     *
     * @param blogId blogId
     * @return 阅读量
     */
    @Override
    public Integer getReadingCountByBlogId(Long blogId) {
        // 首先：空数据判断
        judgeBlogInfoNull(blogId);

        Object object = redisTemplate.opsForHash().get(CacheConstant.READING_COUNT, blogId);
        if (!Objects.isNull(object)) {
            try {
                return JSONObject.parseObject(object.toString(), Integer.class);
            } catch (Exception e) {
                log.info("【获取阅读量】 json->Integer 转换失败");
            }
        }
        // 从数据库中刷新到缓存
        return getBlogEvaluationFromMysql(blogId).getReadingCount();
    }

    /**
     * 根据 blogId 获取 comment list
     *
     * @param blogId blogId
     * @return comment list
     */
    @Override
    public List<Comment> getBlogCommentListByBlogId(Long blogId) {
        // 首先：空数据判断
        judgeBlogInfoNull(blogId);

        String commentKey = BlogCacheUtil.generateKey(CacheConstant.COMMENT, blogId);
        List<Object> commentObj = redisTemplate.opsForHash().values(commentKey);
        try {
            return commentObj.stream().map(
                    comment -> JSONObject.parseObject(comment.toString(), Comment.class)).collect(Collectors.toList()
            );
        } catch (Exception e) {
            log.info("【获取评论列表】 json->Comment 转换失败");
        }
        return getBlogEvaluationFromMysql(blogId).getComments();
    }

    /**
     * 根据 blogId 获取 vote list
     *
     * @param blogId blogId
     * @param userId userId
     * @return vote list
     */
    @Override
    public Long judgeVotedById(Long blogId, Integer userId) {
        String voteKey = BlogCacheUtil.generateKey(CacheConstant.VOTE, blogId);
        String userKey = BlogCacheUtil.generateKey(CacheConstant.USER_ID, userId);
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/judgevoted.lua"));
        redisScript.setResultType(Long.class);
        ArrayList<String> voteKeyList = new ArrayList<>(1);
        ArrayList<String> userKeyList = new ArrayList<>(1);
        voteKeyList.add(voteKey);
        userKeyList.add(userKey);
        return redisTemplate.execute(redisScript, voteKeyList, userKeyList);
    }

    /**
     * @param blogId blogId
     * @param voteId voteId
     * @return vote
     */
    @Override
    public Vote getVotedByBlogId2VoteId(Long blogId, Long voteId) {
        // 首先空数据检验
        judgeBlogInfoNull(blogId);

        String voteKey = BlogCacheUtil.generateKey(CacheConstant.VOTE, blogId);
        Boolean exist;
        if (!Objects.isNull(exist = redisTemplate.hasKey(voteKey)) && exist) {
            try {
                Object object = redisTemplate.opsForHash().get(voteKey, voteId);
                if (Objects.isNull(object)) {
                    return null;
                }
                return JSONObject.parseObject(object.toString(), Vote.class);
            } catch (Exception e) {
                log.error("【获取 vote】 json -> Vote 类型转换异常:{}", e.getMessage());
            }
        }
        // 从数据库刷新到缓存
        List<Vote> votes = getBlogEvaluationFromMysql(blogId).getVotes();
        for (Vote vote : votes) {
            if (vote.getId().equals(voteId)) {
                return vote;
            }
        }
        return null;
    }

    /**
     * 检测：请求的博客是否为 空数据
     *
     * @param blogId blogId
     */
    private void judgeBlogInfoNull(Long blogId) {
        if (Objects.isNull(blogId)) {
            // todo
            log.error("参数为 null");
            throw new NullBlogException(ValidateConstant.NULL_BLOG_INFO);
        }
        Boolean judge = redisTemplate.hasKey(blogId.toString());
        if (!Objects.isNull(judge) && judge) {
            redisTemplate.expire(blogId.toString(), CacheConstant.EXPIRE, TimeUnit.SECONDS);
            throw new NullBlogException("Blog：" + blogId + " " + ValidateConstant.NULL_BLOG_INFO);
        }
    }

    /**
     * 检测是否缓存已存在
     *
     * @param blogId blogId
     * @return dto
     */
    private BlogEvaluationCacheDTO judgeBlogNull(Long blogId) {
        judgeBlogInfoNull(blogId);
        return getBlogEvaluationByBlogId2Redis(blogId);
    }

    /**
     * 从数据库中刷新缓存
     *
     * @param blogId blogId
     * @return blogEvaluation
     */
    private BlogEvaluationCacheDTO flushCacheByMySQL(Long blogId) {
        // 从数据库纵获取数据
        BlogEvaluationCacheDTO blogEvaluation = blogEvaluationRepository.findByBlogId(blogId);
        if (Objects.isNull(blogEvaluation)) {
            // todo 防止 缓存穿透 处理
            redisTemplate.opsForValue().set(String.valueOf(blogId), CacheConstant.NULL, CacheConstant.EXPIRE,
                    TimeUnit.SECONDS);
            throw new NullBlogException("Blog：" + blogId + " " + ValidateConstant.NULL_BLOG_INFO);
        }
        blogEvaluation.setBlogId(blogId);
        // 从 mysql 中获取时使用的是 左连接 left join，所以 表之间关联时，comment以及vote表中可能没有数据，此时依旧会导出 null 值
        if (Objects.isNull(blogEvaluation.getVoteCount()) || blogEvaluation.getVoteCount() == 0 ||
                Objects.isNull(blogEvaluation.getVotes()) || blogEvaluation.getVotes().size() == 0) {
            blogEvaluation.setVoteCount(0);
            blogEvaluation.setVotes(new ArrayList<>(1));
        }
        if (Objects.isNull(blogEvaluation.getCommentCount()) || blogEvaluation.getCommentCount() == 0 ||
                Objects.isNull(blogEvaluation.getComments()) || blogEvaluation.getComments().size() == 0) {
            blogEvaluation.setCommentCount(0);
            blogEvaluation.setComments(new ArrayList<>(1));
        }
        // 刷新到缓存
        saveBlogEvaluation2Redis(blogId, blogEvaluation);
        return blogEvaluation;
    }

    /**
     * 从 数据库中获取 blog evaluation ：从 mysql 中查找
     *
     * @param blogId blogId
     * @return dto
     */
    @Override
    public BlogEvaluationCacheDTO getBlogEvaluationFromMysql(Long blogId) {
        BlogEvaluationCacheDTO blogEvaluationCache;
        if (limitFlowLock.limitRequestPass(blogId, () -> Objects.isNull(judgeBlogNull(blogId)), false)) {
            try {
                blogEvaluationCache = flushCacheByMySQL(blogId);
            } finally {
                // 保证锁被释放
                limitFlowLock.releasePermission(blogId);
            }
        } else {
            // 这部分线程 是被阻塞后，被唤醒的
            blogEvaluationCache = judgeBlogNull(blogId);
            if (Objects.isNull(blogEvaluationCache)) {
                throw new LimitFlowException("Blog:" + blogId + " " + ConcurrencyFlowConstant.SYSTEM_BUSY_MSG);
            }
        }
        return blogEvaluationCache;
    }

    /**
     * 将 数据 更新入 数据库中 ： 定时更新入 mysql
     * 只更新 readingCount、commentCount、voteCount
     */
    @Override
    public void saveBlogEvaluation2Mysql() {
        // readCount
        Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(CacheConstant.READING_COUNT,
                ScanOptions.NONE);
        Map<Long, Long> readCountMap = new HashMap<>(1);
        Long blogId;
        Long readCount = null;
        Map.Entry<Object, Object> entry;
        while (cursor.hasNext()) {
            entry = cursor.next();
            try {
                blogId = JSONObject.parseObject(entry.getKey().toString(), Long.class);
                readCount = JSONObject.parseObject(entry.getValue().toString(), Long.class);
                readCountMap.put(blogId, readCount);
            } catch (Exception e) {
                // todo 类型转换异常
                log.error("【 readCount ：redis -> mysql 】类型转换异常:{}", e.getMessage());
            }
        }
        // commentCount
        cursor = redisTemplate.opsForHash().scan(CacheConstant.COMMENT_COUNT, ScanOptions.NONE);
        Map<Long, Long> commentCountMap = new HashMap<>(1);
        Long commentCount;
        while (cursor.hasNext()) {
            entry = cursor.next();
            try {
                blogId = JSONObject.parseObject(entry.getKey().toString(), Long.class);
                commentCount = JSONObject.parseObject(entry.getValue().toString(), Long.class);
                commentCountMap.put(blogId, commentCount);
            } catch (Exception e) {
                // todo 类型转换异常
                log.error("【 commentCount ：redis -> mysql 】类型转换异常:{}", e.getMessage());
            }
        }
        // voteCount
        cursor = redisTemplate.opsForHash().scan(CacheConstant.VOTE_COUNT, ScanOptions.NONE);
        Map<Long, Long> voteCountMap = new HashMap<>(1);
        Long voteCount;
        while (cursor.hasNext()) {
            entry = cursor.next();
            try {
                blogId = JSONObject.parseObject(entry.getKey().toString(), Long.class);
                voteCount = JSONObject.parseObject(entry.getValue().toString(), Long.class);
                voteCountMap.put(blogId, voteCount);
            } catch (Exception e) {
                // todo 类型转换异常
                log.error("【 voteCount ：redis -> mysql 】类型转换异常:{}", e.getMessage());
            }
        }
        // 单独抽离出去加事务，防止耗时的操作占用事务，导致 SQL 连接被耗尽
        saveBlogEvaluation(readCountMap, commentCountMap, voteCountMap);
    }

    /**
     * 单独抽离出来添加事务：避免和 耗时的操作混合在一起，导致 SQL 连接被耗尽
     *
     * @param readCountMap    阅读量 模块
     * @param commentCountMap 评论量 模块
     * @param voteCountMap    点赞量 模块
     */
    @Transactional(rollbackFor = Throwable.class)
    void saveBlogEvaluation(Map<Long, Long> readCountMap, Map<Long, Long> commentCountMap, Map<Long, Long>
            voteCountMap) {
        if (readCountMap.size() > 0 || commentCountMap.size() > 0 || voteCountMap.size() > 0) {
            log.info("readCount:{}  |  commentCount:{}   |   voteCount:{}", readCountMap, commentCountMap,
                    voteCountMap);
            blogEvaluationRepository.saveBlogEvaluation(readCountMap, commentCountMap, voteCountMap);
        }
    }

    /**
     * 获取 blog evaluation
     * 从 redis 中获取 blog evaluation ：当 redis 中获取不到时，再从 mysql 中
     *
     * @param blogId blogId
     * @return blog evaluation blog evaluation
     */
    @Override
    public BlogEvaluationCacheDTO getBlogEvaluationByBlogId2Redis(Long blogId) {
        BlogEvaluationCacheDTO blogEvaluation = new BlogEvaluationCacheDTO();
        Object readCountObj = redisTemplate.opsForHash().get(CacheConstant.READING_COUNT, blogId);
        Object commentCountObj = redisTemplate.opsForHash().get(CacheConstant.COMMENT_COUNT, blogId);
        Object voteCountObj = redisTemplate.opsForHash().get(CacheConstant.VOTE_COUNT, blogId);
        if (Objects.isNull(readCountObj) || Objects.isNull(commentCountObj) || Objects.isNull(voteCountObj)) {
            log.info("【缓存】blog:{} 无缓存", blogId);
            return null;
        }
        try {
            /*
            从 redis 中获取的数据，需要经过 JSONObject 转换
            否则报错：com.alibaba.fastjson.JSONObject cannot be cast to com.study.blog.dto.BlogEvaluationCacheDTO
             */
            blogEvaluation.setReadingCount(JSONObject.parseObject(readCountObj.toString(), Integer.class));
            blogEvaluation.setCommentCount(JSONObject.parseObject(commentCountObj.toString(), Integer.class));
            blogEvaluation.setVoteCount(JSONObject.parseObject(voteCountObj.toString(), Integer.class));
        } catch (Exception e) {
            log.error("【缓存】blog:{} Json 转换异常", blogId, e.getMessage());
            return null;
        }
        return blogEvaluation;
    }

    /**
     * 将 数据 保存到 redis 中 ： 新建：点赞、评论
     *
     * @param blogId         blogId
     * @param blogEvaluation blog evaluation
     */
    @Override
    public void saveBlogEvaluation2Redis(Long blogId, BlogEvaluationCacheDTO blogEvaluation) {
        redisTemplate.opsForHash().put(CacheConstant.READING_COUNT, blogId, blogEvaluation.getReadingCount());
        redisTemplate.opsForHash().put(CacheConstant.COMMENT_COUNT, blogId, blogEvaluation.getCommentCount());
        redisTemplate.opsForHash().put(CacheConstant.VOTE_COUNT, blogId, blogEvaluation.getVoteCount());
        blogEvaluation.getComments().forEach(comment -> {
            String commentKey = BlogCacheUtil.generateKey(CacheConstant.COMMENT, blogId);
            redisTemplate.opsForHash().put(commentKey, comment.getId(), comment);
        });
        blogEvaluation.getVotes().forEach(vote -> {
            String voteKey = BlogCacheUtil.generateKey(CacheConstant.VOTE, blogId);
            redisTemplate.opsForHash().put(voteKey, vote.getId(), vote);
        });
    }

    /**
     * 新增 评论 =》评论量
     *
     * @param comment comment
     */
    @Override
    public Long addBlogComment(Comment comment) {
        Long blogId = comment.getBlogId();

        // 插入到 mysql 中获取 commentId
        blogEvaluationRepository.insertComment(comment);
        // 直接 put 到 redis ：不会涉及到旧的状态，没有并发问题！
        String commentKey = BlogCacheUtil.generateKey(CacheConstant.COMMENT, blogId);
        redisTemplate.opsForHash().put(commentKey, comment.getId(), comment);
        // 评论量+1：redis单线程操作，不会出现并发问题！
        return redisTemplate.opsForHash().increment(CacheConstant.COMMENT_COUNT, blogId, 1L);
    }

    /**
     * 删除 评论 =》评论量
     *
     * @param blogId    blogId
     * @param commentId commentId
     */
    @Override
    public Long deleteBlogComment(Long blogId, Long commentId) {
        // redis 删除评论！
        String commentKey = BlogCacheUtil.generateKey(CacheConstant.COMMENT, blogId);
        redisTemplate.opsForHash().delete(commentKey, commentId);
        // 删除 mysql 中的记录
        blogEvaluationRepository.deleteCommentByCommentId(commentId);
        // 评论量 -1
        return redisTemplate.opsForHash().increment(CacheConstant.COMMENT_COUNT, blogId, -1L);
    }

    /**
     * 新增 点赞 =》点赞量
     *
     * @param vote vote
     */
    @Override
    public Long voteBlog(Vote vote) {
        Long blogId = vote.getBlogId();
        // 获取 voteId
        blogEvaluationRepository.insertVote(vote);
        // redis 新增vote
        String voteKey = BlogCacheUtil.generateKey(CacheConstant.VOTE, blogId);
        redisTemplate.opsForHash().put(voteKey, vote.getId(), vote);
        // 修改 voteCount 并返回
        return redisTemplate.opsForHash().increment(CacheConstant.VOTE_COUNT, blogId, 1L);
    }

    /**
     * 取消 点赞 =》点赞量
     *
     * @param blogId blogId
     * @param voteId voteId
     */
    @Override
    public Long cancelVotedBlog(Long blogId, Long voteId) {
        // 删除 redis 中的 vote 记录
        String voteKey = BlogCacheUtil.generateKey(CacheConstant.VOTE, blogId);
        redisTemplate.opsForHash().delete(voteKey, voteId);
        // 删除 mysql 中的记录
        blogEvaluationRepository.deleteVoteByVoteId(voteId);
        // 点赞量更新 -1
        return redisTemplate.opsForHash().increment(CacheConstant.VOTE_COUNT, blogId, -1L);
    }

    /**
     * 访问 blog =》阅读量
     *
     * @param blogId blogId
     * @return 阅读量
     */
    @Override
    public Long incrementBlogReading(Long blogId) {
        // 首先：空数据判断
        judgeBlogInfoNull(blogId);

        // 判断键值是否存在
        if (!redisTemplate.opsForHash().hasKey(CacheConstant.READING_COUNT, blogId)) {
            // 从数据库刷新到 缓存(如果查询结果为 null，会抛出异常)
            getBlogEvaluationFromMysql(blogId);
        }
        return redisTemplate.opsForHash().increment(CacheConstant.READING_COUNT, blogId, 1L);
    }

    /**
     * 清除博客记录
     *
     * @param blogId blogId
     */
    @Override
    public void deleteBlogEvaluation(Long blogId) {
        // 首先：判断是否空数据
        judgeBlogInfoNull(blogId);

        redisTemplate.opsForHash().delete(CacheConstant.READING_COUNT, blogId);
        redisTemplate.opsForHash().delete(CacheConstant.COMMENT_COUNT, blogId);
        redisTemplate.opsForHash().delete(CacheConstant.VOTE_COUNT, blogId);
        String commentKey = BlogCacheUtil.generateKey(CacheConstant.COMMENT, blogId);
        String voteKey = BlogCacheUtil.generateKey(CacheConstant.VOTE, blogId);
        redisTemplate.delete(commentKey);
        redisTemplate.delete(voteKey);
    }
}
