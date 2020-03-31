package com.study.blog.service.impl;

import com.github.pagehelper.Page;
import com.study.blog.constant.EvaluationConstant;
import com.study.blog.dto.BlogEvaluationCacheDTO;
import com.study.blog.entity.Blog;
import com.study.blog.entity.EsBlog;
import com.study.blog.entity.User;
import com.study.blog.exception.NullBlogException;
import com.study.blog.repository.BlogRepository;
import com.study.blog.repository.EsBlogRepository;
import com.study.blog.service.BlogEvaluationCacheService;
import com.study.blog.service.BlogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    public BlogServiceImpl(BlogRepository blogRepository, EsBlogRepository esBlogRepository,
                           BlogEvaluationCacheService cacheService, RedisTemplate<String, Object> redisTemplate) {
        this.blogRepository = blogRepository;
        this.esBlogRepository = esBlogRepository;
        this.cacheService = cacheService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Blog getBlogById(Long id) {
        Boolean judge = redisTemplate.hasKey(id.toString());
        if (!Objects.isNull(judge) && judge) {
            // 说明 该blog 不存在！
            throw new NullBlogException("blog 不存在！");
        }

        Blog blog = blogRepository.getBlogById(id);
        if (Objects.isNull(blog)) {
            // todo 防止 缓存穿透 处理
            log.error("【获取 blog 信息】id 为 {} 的 blog 不存在！", id);
            redisTemplate.opsForValue().set(Long.toString(id), EvaluationConstant.NULL,
                    EvaluationConstant.EXPIRE, TimeUnit.SECONDS);
            throw new NullBlogException("blog 不存在！");

        }
        blogCacheEvaluation(blog);
        return blog;
    }

    /**
     * 保存博客
     *
     * @param blog 博客
     * @return blog
     */
    @Transactional(rollbackFor = Throwable.class)
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
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Blog updateBlog(Blog blog, User user) {
        blogRepository.updateBlog(blog);
        // todo
        blog.setCreateTime(blogRepository.getCreateTime(blog.getBlogId()));
        updateEsBlog(blog, user);
        return blog;
    }

    /**
     * 删除博客
     *
     * @param blogId blog id
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void removeBlog(Long blogId) {
        cacheService.deleteBlogEvaluation(blogId);
        blogRepository.removeBlog(blogId);
        // 同时删除 ES 中的博客记录
        EsBlog esBlog = esBlogRepository.findByBlogId(blogId);
        esBlogRepository.deleteById(esBlog.getId());
        // 同时删除缓存中的记录
    }

    /**
     * 阅读量+1
     *
     * @param id 博客id
     */
    @Transactional(rollbackFor = Throwable.class)
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
            log.error("【保存更新 ES】：{}", e.getMessage());
            return;
        }
        if (esBlog == null) {
            log.error("【保存更新 ES】更新博客 ： EsBlog 检索为NULL ");
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
            log.error("【保存更新 ES】阅读量 ：{}", e.getMessage());
            return;
        }
        if (esBlog == null) {
            log.error("【保存更新 ES】阅读量 ：EsBlog 检索为NULL");
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
        // TitleLike  UserOrTagsLike UserOrder  CreateTimeDesc(title,user, tags,user, pageable);
        log.info("【博客列表】 userId={}", userId);
        if (title != null) {
            title = "%" + title + "%";
        }
        log.info("title:{}", title);
        Page<Blog> blogPage = blogRepository.findByTitleLikeAndOrderByTimeDesc(userId, title, startPage, pageSize);
/*        for (Blog blog : blogPage) {
            blogCacheEvaluation(blog);
        }*/
        return blogPage;
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
        Page<Blog> blogPage = blogRepository.findByUserAndTitleLike(userId, title, startPage, pageSize);
/*        for (Blog blog : blogPage) {
            blogCacheEvaluation(blog);
        }*/
        return blogPage;
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
        Page<Blog> blogPage = blogRepository.findByCatalog(catalogId, pageNum, pageSize);
/*        for (Blog blog : blogPage) {
            blogCacheEvaluation(blog);
        }*/
        return blogPage;
    }

    /**
     * 用缓存中的信息更新 mysql 中的信息
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
                cacheDTO = cacheService.getBlogEvaluationByBlogId2Mysql(blog.getBlogId());
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
