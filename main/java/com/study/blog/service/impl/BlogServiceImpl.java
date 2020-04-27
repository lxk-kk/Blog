package com.study.blog.service.impl;

import com.github.pagehelper.Page;
import com.study.blog.constant.CacheConstant;
import com.study.blog.constant.ConcurrentConstant;
import com.study.blog.constant.ValidateConstant;
import com.study.blog.dto.BlogEvaluationCacheDTO;
import com.study.blog.entity.Blog;
import com.study.blog.entity.EsBlog;
import com.study.blog.entity.User;
import com.study.blog.exception.LimitFlowException;
import com.study.blog.exception.NullBlogException;
import com.study.blog.lock.LimitFlowLock2Park;
import com.study.blog.lock.LimitFlowLock2Wait;
import com.study.blog.repository.BlogRepository;
import com.study.blog.repository.es2search.EsBlogRepository;
import com.study.blog.service.BlogCacheService;
import com.study.blog.service.BlogEvaluationCacheService;
import com.study.blog.service.BlogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author 10652
 */
@Service
@Slf4j
public class BlogServiceImpl implements BlogService {
    private final BlogRepository blogRepository;
    private final EsBlogRepository esBlogRepository;
    private final BlogEvaluationCacheService cacheService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BlogCacheService blogCacheService;
    private final LimitFlowLock2Wait limitFlowLock2Wait;
    private LimitFlowLock2Park limitFlowLock2Park;

    @Autowired
    public BlogServiceImpl(BlogRepository blogRepository,
                           EsBlogRepository esBlogRepository,
                           BlogEvaluationCacheService cacheService,
                           RedisTemplate<String, Object> redisTemplate,
                           BlogCacheService blogCacheService) {
        this.blogRepository = blogRepository;
        this.esBlogRepository = esBlogRepository;
        this.cacheService = cacheService;
        this.redisTemplate = redisTemplate;
        this.blogCacheService = blogCacheService;
        this.limitFlowLock2Park = getLimitFlowLock2Park();
        this.limitFlowLock2Wait = getLimitFlowLock2Wait();
    }

    private LimitFlowLock2Wait getLimitFlowLock2Wait() {
        LimitFlowLock2Wait limitFlowLock2Wait = new LimitFlowLock2Wait();
        log.info("【Lock_EVALUATION】:{}", limitFlowLock2Wait);
        return limitFlowLock2Wait;
    }

    private LimitFlowLock2Park getLimitFlowLock2Park() {
        LimitFlowLock2Park limitFlowLock2Park = new LimitFlowLock2Park();
        log.info("【Lock_BLOG】:{}", limitFlowLock2Park);
        return limitFlowLock2Park;
    }

    @Override
    public Blog getBlogById(Long blogId) {
        Blog blog;
        // 尝试从缓存中获取博客，如果是空数据，则会抛出异常！
        if (Objects.isNull(blog = judgeBlogNull(blogId))) {
            // 限流
            if (limitFlowLock2Wait.limitRequestPass(blogId, () -> Objects.isNull(judgeBlogNull(blogId)), false)) {
                try {
                    blog = flushCacheByMySQL(blogId);
                } finally {
                    // 保证获取许可证的线程 释放许可证！
                    // 保证许可证被释放！
                    // limitFlowLock2Park.releaseLock(blogId);
                    limitFlowLock2Wait.releaseLock(blogId);
                }
            } else {
                // 这部分线程是被限流被唤醒的!
                // 尝试从缓存中获取博客，如果是空数据，则会抛出异常，不再继续往下执行！
                blog = judgeBlogNull(blogId);
                if (Objects.isNull(blog)) {
                    // 如果被阻塞后还是为 null，说明现在请求资源的线程数太多了，返回系统正忙！
                    throw new LimitFlowException("Blog:" + blogId + " " + ConcurrentConstant.SYSTEM_BUSY_MSG);
                }
            }
        }
        // 使用 缓存中的指标量展示博客
        blogCacheEvaluation(blog);
        return blog;
    }

    /**
     * 检查是否是空数据，如果是，则直接抛出异常，如果不是，则继续执行
     *
     * @param blogId blogId
     */
    private Blog judgeBlogNull(Long blogId) {
        Boolean judge = redisTemplate.hasKey(blogId.toString());
        // 检查是否是 空 数据！
        if (!Objects.isNull(judge) && judge) {
            redisTemplate.expire(blogId.toString(), CacheConstant.EXPIRE, TimeUnit.SECONDS);
            // 说明 该blog 不存在！
            throw new NullBlogException("Blog：" + blogId + " " + ValidateConstant.NULL_BLOG_INFO);
        }
        // 尝试从缓存中查询
        return blogCacheService.getBlogFromCacheById(blogId);
    }

    /**
     * 从 数据库 中刷新数据到 缓存
     *
     * @param blogId blogId
     * @return blog
     */
    private Blog flushCacheByMySQL(Long blogId) {
        // 从 数据库中获取
        Blog blog;
        try {
            blog = blogRepository.getBlogById(blogId);
        } finally {
            // 首先释放许可证：提高系统吞吐量！
            // 保证许可证被释放！
            // limitFlowLock2Park.releasePermission();
            limitFlowLock2Wait.releasePermission();
        }
        if (Objects.isNull(blog)) {
            // 说明数据库中也不存在，防止 缓存穿透 处理
            log.error("【获取 博客】blogId 为 {} 的 blog 不存在！", blogId);
            redisTemplate.opsForValue().set(String.valueOf(blogId), CacheConstant.NULL, CacheConstant.EXPIRE,
                    TimeUnit.SECONDS);
            throw new NullBlogException("Blog：" + blogId + " " + ValidateConstant.NULL_BLOG_INFO);
        }
        // 使缓存生效
        blogCacheService.putBlogCache(blog);
        return blog;
    }

    /**
     * 保存博客
     *
     * @param blog 博客
     * @return blog
     */
    @Override
    public Blog saveBlog(Blog blog, User user) {
        // 必须先新增到关系型数据库中，使得博客id回传
        blogRepository.saveBlog(blog);
        log.info("【保存博客】{}", blog.getBlogId());
        blog.setCreateTime(blogRepository.getCreateTime(blog.getBlogId()));
        EsBlog esBlog = new EsBlog(blog, user);
        // 保存到关系性数据库的同时保存到ES中
        esBlogRepository.save(esBlog);
        log.info("【保存博客】 到 ES");
        return blog;
    }

    /**
     * 更新博客
     *
     * @param blog 博客
     * @param user 博主
     * @return 博客
     */
    @Override
    public Blog updateBlog(Blog blog, User user) {
        // 先删除缓存，再更新数据库
        blogCacheService.removeBlogFromCacheById(blog.getBlogId());
        blogRepository.updateBlog(blog);
        // todo
        blog.setCreateTime(blogRepository.getCreateTime(blog.getBlogId()));
        // 更新 ES
        updateEsBlog(blog, user);
        return blog;
    }

    /**
     * 删除博客
     *
     * @param blogId blog id
     */
    @Override
    public void removeBlog(Long blogId) {
        // 删除博客缓存
        blogCacheService.removeBlogFromCacheById(blogId);
        // 删除博客相关指标量
        cacheService.deleteBlogEvaluation(blogId);
        // 数据库删除缓存
        blogRepository.removeBlog(blogId);
        // 删除 ES 中的博客记录
        EsBlog esBlog = esBlogRepository.findByBlogId(blogId);
        esBlogRepository.deleteById(esBlog.getId());
    }

    /**
     * 阅读量+1
     *
     * @param id 博客id
     */
    @Override
    public void readingIncrement(Long id) {
        // blogRepository.readingIncrement(id);
        Long count = cacheService.incrementBlogReading(id);
        count = Objects.isNull(count) ? 0L : count;
        Integer readCount = Math.toIntExact(count);
        updateEsBlog(id, readCount);
    }

    /**
     * 更新 ES Blog
     *
     * @param blog 博客
     * @param user 博主
     */
    private void updateEsBlog(Blog blog, User user) {
        EsBlog esBlog = null;
        try {
            esBlog = esBlogRepository.findByBlogId(blog.getBlogId());
        } catch (Exception e) {
            log.error("【ES】查询异常：{}", e.getMessage());
            // todo 抛出异常
            return;
        }
        if (esBlog == null) {
            // log.error("【ES】不存在 Blog:{}", blog.getBlogId());
            // todo 抛出异常
            return;
        }
        esBlog.update(blog, user);
        esBlogRepository.save(esBlog);
    }

    /**
     * 更新 ES blog
     *
     * @param blogId 博客id
     */
    private void updateEsBlog(Long blogId, Integer readCount) {
        EsBlog esBlog = null;
        try {
            esBlog = esBlogRepository.findByBlogId(blogId);
        } catch (Exception e) {
            log.error("【ES】阅读量 ：{}", e.getMessage());
            return;
        }
        if (esBlog == null) {
            // log.error("【ES】不存在 blog:{}", blogId);
            return;
        }
       /*
       注意这里要避免 NPE 不能使用 long 类型，应该使用 包装类 Long 并且判空
       Long oldRead = esBlog.getReadCount()
        if (oldRead == null || oldRead == 0) {
            esBlog.setReadCount((long) 1)
        } else {
            esBlog.setReadCount(oldRead + 1)
        }
        */
        esBlog.setReadCount((long) readCount);
        esBlogRepository.save(esBlog);
    }

    /**
     * 最新查询
     *
     * @param userId    用户id
     * @param title     博客标题
     * @param startPage 起始页
     * @param pageSize  页大小
     * @return blogs
     */
    @Override
    public Page<Blog> listBlogsByTitleLikeAndDescSort(int userId, String title, int startPage, int pageSize) {
        // 模糊查询
        log.info("【博客列表】 userId={}", userId);
        if (title != null) {
            title = "%" + title + "%";
        }
        log.info("title:{}", title);
        return blogRepository.findByTitleLikeAndOrderByTimeDesc(userId, title, startPage, pageSize);
    }

    /**
     * 最热查询
     *
     * @param userId    用户id
     * @param title     标题
     * @param startPage 起始页
     * @param pageSize  页大小
     * @return blogs
     */
    @Override
    public Page<Blog> listBlogsByTitleLike(int userId, String title, int startPage, int pageSize) {
        // 模糊查询
        if (title != null) {
            title = "%" + title + "%";
        }
        return blogRepository.findByUserAndTitleLike(userId, title, startPage, pageSize);
    }

    /**
     * 根据分类查询博客
     *
     * @param catalogId 分类id
     * @param pageNum   起始页
     * @param pageSize  页大小
     * @return blog list
     */
    @Override
    public Page<Blog> listBlogByCatalog(Long catalogId, int pageNum, int pageSize) {
        return blogRepository.findByCatalog(catalogId, pageNum, pageSize);
    }

    /**
     * 使用缓存中的 指标量 展示博客
     *
     * @param blog blog
     */
    private void blogCacheEvaluation(Blog blog) {
        // 从缓存中获取 阅读量、点赞量、评论量
        // 先查询 缓存
        BlogEvaluationCacheDTO cacheDTO = cacheService.getBlogEvaluationByBlogId2Redis(blog.getBlogId());
        if (Objects.isNull(cacheDTO)) {
            // 缓存中不存在：查询mysql
            try {
                log.info("【数据库】 查询 blog:{}", blog.getBlogId());
                cacheDTO = cacheService.getBlogEvaluationFromMysql(blog.getBlogId());
            } catch (NullBlogException e) {
                // todo 获取博客列表应该不会出现这种情况
                log.error("【数据库】 blog:{} 不存在 ", blog.getBlogId());
            }
        }
        Integer readCount = cacheDTO.getReadingCount();
        Integer commentCount = cacheDTO.getCommentCount();
        Integer voteCount = cacheDTO.getVoteCount();
        blog.setReadCount((long) readCount);
        blog.setCommentCount((long) commentCount);
        blog.setLikeCount((long) voteCount);
    }
}
