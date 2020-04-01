package com.study.blog.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.study.blog.constant.EvaluationConstant;
import com.study.blog.dto.BlogEvaluationCacheDTO;
import com.study.blog.entity.Comment;
import com.study.blog.entity.Vote;
import com.study.blog.exception.NullBlogException;
import com.study.blog.repository.BlogEvaluationRepository;
import com.study.blog.service.BlogEvaluationCacheService;
import com.study.blog.util.BlogEvaluationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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


    @Autowired
    public BlogEvaluationCacheServiceImpl(RedisTemplate<String, Object> redisTemplate, BlogEvaluationRepository
            blogEvaluationRepository) {
        this.redisTemplate = redisTemplate;
        this.blogEvaluationRepository = blogEvaluationRepository;
    }

    /**
     * 获取 阅读量
     *
     * @param blogId blogId
     * @return 阅读量
     */
    @Override
    public Integer getReadingCountByBlogId(Long blogId) {
        if (Objects.isNull(blogId)) {
            // todo
            log.error("【获取阅读量】 blogId 为空");
        }
        Object object = redisTemplate.opsForHash().get(EvaluationConstant.READING_COUNT, blogId);
        boolean judge = true;
        if (Objects.isNull(object)) {
            // 从数据库中获取
            judge = false;
        }
        Integer readingCount = null;
        if (judge) {
            try {
                readingCount = JSONObject.parseObject(object.toString(), Integer.class);
            } catch (Exception e) {
                log.info("【获取阅读量】 json->Integer 转换失败");
                judge = false;
            }
        }
        if (judge) {
            return readingCount;
        }
        return getBlogEvaluationByBlogId2Mysql(blogId).getReadingCount();
    }

    /**
     * 根据 blogId 获取 comment list
     *
     * @param blogId blogId
     * @return comment list
     */
    @Override
    public List<Comment> getBlogCommentListByBlogId(Long blogId) {
        List<Comment> comments;
        if (Objects.isNull(blogId)) {
            // todo
            log.error("【获取评论列表】 blogId 为空");
        }
        String commentKey = BlogEvaluationUtil.generateKey(EvaluationConstant.COMMENT, blogId);
        List<Object> commentObj = redisTemplate.opsForHash().values(commentKey);
        try {
            comments = commentObj.stream().map(
                    comment -> JSONObject.parseObject(comment.toString(), Comment.class)).collect(Collectors.toList()
            );
        } catch (Exception e) {
            log.info("【获取评论列表】 json->Comment 转换失败");
            comments = getBlogEvaluationByBlogId2Mysql(blogId).getComments();
        }
        return comments;
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
        String voteKey = BlogEvaluationUtil.generateKey(EvaluationConstant.VOTE, blogId);
        String userKey = BlogEvaluationUtil.generateKey(EvaluationConstant.USER_ID, userId);
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
        String voteKey = BlogEvaluationUtil.generateKey(EvaluationConstant.VOTE, blogId);
        Boolean exist = redisTemplate.hasKey(voteKey);
        Vote vote = null;
        if (Objects.isNull(exist) || !exist) {
            // 缓存中不存在：从 mysql 中获取
            getBlogEvaluationByBlogId2Mysql(blogId);
        }
        try {
            Object object = redisTemplate.opsForHash().get(voteKey, voteId);
            if (!Objects.isNull(object)) {
                vote = JSONObject.parseObject(object.toString(), Vote.class);
            }
        } catch (Exception e) {
            log.error("【获取 vote】 json -> Vote 类型转换异常:{}", e.getMessage());
        }
        return vote;
    }

    /**
     * 从 数据库中获取 blog evaluation ：从 mysql 中查找
     *
     * @param blogId blogId
     * @return dto
     */
    @Override
    public BlogEvaluationCacheDTO getBlogEvaluationByBlogId2Mysql(Long blogId) {
        if (Objects.isNull(blogId)) {
            // todo
            log.error("【查询 数据库】 blogId 为空");
            return null;
        }
        Boolean judge = redisTemplate.hasKey(blogId.toString());
        if (!Objects.isNull(judge) && judge) {
            throw new NullBlogException("blog 不存在！");
        }
        BlogEvaluationCacheDTO blogEvaluation = null;

        // 考虑缓存并发问题【双重检测锁】：防止缓存中不存在，进而在短时间内向数据库发起大量请求，对数据库造成冲击！
        /*
        这一步已经在调用的方法中展现！
        if (!Objects.isNull(blogEvaluation)) {
            return blogEvaluation;
        }
        */
        /*
            若缓存不存在，则从数据库中获取数据的时候，进行加锁，锁内再次从redis获取进行判断，获取完数据后将数据存入 缓存！
            加锁：只有一个进程能从数据库获取数据，其余进程只能在锁外等着（要不要换成分布式锁！）
            再次从redis中尝试获取数据进行判断：后续进入if(!judge)了的线程不需要从数据库中获取了，可以直接从redis中获取！
         */

        log.info("【缓存穿透：查询mysql】 synchronized 锁外 thread ：{}", Thread.currentThread().getId());

        synchronized (this) {
            // 注意：synchronized(this)：表示锁代码块，作用对象为执行该方法的对象，不同对象之间互不影响
            // 由于 这是在 service 中，业务领域对象 service 为单例，所以，所有请求，所有客户端请求，都会通过该方法执行此代码块：所以有用！
            // 由于 此处 blogEvaluation 对象是局部变量，不受多线程的影响，所以每个线程的 blogEvaluation是不同的，是线程安全的！

            log.info("【缓存穿透：查询mysql】锁内 thread ：{}", Thread.currentThread().getId());

            // 排除线程已经进入方法块，但是缓存已经存在的情况
            if (Objects.isNull(blogEvaluation = getBlogEvaluationByBlogId2Redis(blogId))) {
                // 以下单进程访问！

                log.info("【缓存穿透：查询mysql】 单进程 thread ：{}", Thread.currentThread().getId());

                // 从数据库中获取 blog evaluation
                blogEvaluation = blogEvaluationRepository.findByBlogId(blogId);
                if (Objects.isNull(blogEvaluation)) {
                    // todo 防止 缓存穿透 处理
                    log.error("【获取 blog 信息】id 为 {} 的 blog 不存在！", blogId);
                    redisTemplate.opsForValue().set(Long.toString(blogId), EvaluationConstant.NULL,
                            EvaluationConstant.EXPIRE, TimeUnit.SECONDS);
                    throw new NullBlogException("blog 不存在！");

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
                // 存入 redis 缓存
                saveBlogEvaluation2Redis(blogId, blogEvaluation);
            }
            return blogEvaluation;
        }
    }

    /**
     * 将 数据 更新入 数据库中 ： 定时更新入 mysql
     * 只更新 readingCount、commentCount、voteCount
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void saveBlogEvaluation2Mysql() {
        // readCount
        Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(EvaluationConstant.READING_COUNT,
                ScanOptions.NONE);
        Map<Long, Long> readCountMap = new HashMap<>(1);
        Long blogId;
        Long readCount = null;
        Map.Entry entry;
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
        cursor = redisTemplate.opsForHash().scan(EvaluationConstant.COMMENT_COUNT, ScanOptions.NONE);
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
        cursor = redisTemplate.opsForHash().scan(EvaluationConstant.VOTE_COUNT, ScanOptions.NONE);
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
        Object readCountObj = redisTemplate.opsForHash().get(EvaluationConstant.READING_COUNT, blogId);
        Object commentCountObj = redisTemplate.opsForHash().get(EvaluationConstant.COMMENT_COUNT, blogId);
        Object voteCountObj = redisTemplate.opsForHash().get(EvaluationConstant.VOTE_COUNT, blogId);

        log.info("【查询缓存】");
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
            log.error("【缓存】blog:{} json 转换异常", blogId, e.getMessage());
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
    @Transactional(rollbackFor = Throwable.class)
    public void saveBlogEvaluation2Redis(Long blogId, BlogEvaluationCacheDTO blogEvaluation) {
        redisTemplate.opsForHash().put(EvaluationConstant.READING_COUNT, blogId, blogEvaluation.getReadingCount());
        redisTemplate.opsForHash().put(EvaluationConstant.COMMENT_COUNT, blogId, blogEvaluation.getCommentCount());
        redisTemplate.opsForHash().put(EvaluationConstant.VOTE_COUNT, blogId, blogEvaluation.getVoteCount());
        blogEvaluation.getComments().forEach(comment -> {
            String commentKey = BlogEvaluationUtil.generateKey(EvaluationConstant.COMMENT, blogId);
            redisTemplate.opsForHash().put(commentKey, comment.getId(), comment);
        });
        blogEvaluation.getVotes().forEach(vote -> {
            String voteKey = BlogEvaluationUtil.generateKey(EvaluationConstant.VOTE, blogId);
            redisTemplate.opsForHash().put(voteKey, vote.getId(), vote);
        });
    }

    /**
     * 新增 评论 =》评论量
     *
     * @param comment comment
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Long addBlogComment(Comment comment) {
        Long blogId = comment.getBlogId();

        // 插入到 mysql 中获取 commentId
        blogEvaluationRepository.insertComment(comment);
        // 直接 put 到 redis ：不会涉及到旧的状态，没有并发问题！
        String commentKey = BlogEvaluationUtil.generateKey(EvaluationConstant.COMMENT, blogId);
        redisTemplate.opsForHash().put(commentKey, comment.getId(), comment);
        // 评论量+1：redis单线程操作，不会出现并发问题！
        return redisTemplate.opsForHash().increment(EvaluationConstant.COMMENT_COUNT, blogId, 1L);
    }

    /**
     * 删除 评论 =》评论量
     *
     * @param blogId    blogId
     * @param commentId commentId
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Long deleteBlogComment(Long blogId, Long commentId) {
        // redis 删除评论！
        String commentKey = BlogEvaluationUtil.generateKey(EvaluationConstant.COMMENT, blogId);
        redisTemplate.opsForHash().delete(commentKey, commentId);
        // 删除 mysql 中的记录
        blogEvaluationRepository.deleteCommentByCommentId(commentId);
        // 评论量 -1
        return redisTemplate.opsForHash().increment(EvaluationConstant.COMMENT_COUNT, blogId, -1L);
    }

    /**
     * 新增 点赞 =》点赞量
     *
     * @param vote vote
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Long voteBlog(Vote vote) {
        Long blogId = vote.getBlogId();
        // 获取 voteId
        blogEvaluationRepository.insertVote(vote);
        // redis 新增vote
        String voteKey = BlogEvaluationUtil.generateKey(EvaluationConstant.VOTE, blogId);
        redisTemplate.opsForHash().put(voteKey, vote.getId(), vote);
        // 修改 voteCount 并返回
        return redisTemplate.opsForHash().increment(EvaluationConstant.VOTE_COUNT, blogId, 1L);
    }

    /**
     * 取消 点赞 =》点赞量
     *
     * @param blogId blogId
     * @param voteId voteId
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Long cancelVotedBlog(Long blogId, Long voteId) {
        // 删除 redis 中的 vote 记录
        String voteKey = BlogEvaluationUtil.generateKey(EvaluationConstant.VOTE, blogId);
        redisTemplate.opsForHash().delete(voteKey, voteId);
        // 删除 mysql 中的记录
        blogEvaluationRepository.deleteVoteByVoteId(voteId);
        // 点赞量更新 -1
        return redisTemplate.opsForHash().increment(EvaluationConstant.VOTE_COUNT, blogId, -1L);
    }

    /**
     * 访问 blog =》阅读量
     *
     * @param blogId blogId
     * @return 阅读量
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Long incrementBlogReading(Long blogId) {
        // 判断键值是否存在
        if (!redisTemplate.opsForHash().hasKey(EvaluationConstant.READING_COUNT, blogId)) {
            // 不存在则从数据库中获取
            getBlogEvaluationByBlogId2Mysql(blogId);
        } else {
            log.info("【reading increment 缓存中存在】thread ：{}", Thread.currentThread().getId());
        }
        return redisTemplate.opsForHash().increment(EvaluationConstant.READING_COUNT, blogId, 1L);

    }

    /**
     * 清除博客记录
     *
     * @param blogId blogId
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteBlogEvaluation(Long blogId) {
        redisTemplate.opsForHash().delete(EvaluationConstant.READING_COUNT, blogId);
        redisTemplate.opsForHash().delete(EvaluationConstant.COMMENT_COUNT, blogId);
        redisTemplate.opsForHash().delete(EvaluationConstant.VOTE_COUNT, blogId);
        String commentKey = BlogEvaluationUtil.generateKey(EvaluationConstant.COMMENT, blogId);
        String voteKey = BlogEvaluationUtil.generateKey(EvaluationConstant.VOTE, blogId);
        redisTemplate.delete(commentKey);
        redisTemplate.delete(voteKey);
    }
}
