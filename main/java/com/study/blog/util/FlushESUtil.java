package com.study.blog.util;

import com.study.blog.entity.Blog;
import com.study.blog.entity.EsBlog;
import com.study.blog.entity.User;
import com.study.blog.repository.es2search.EsBlogRepository;
import com.study.blog.service.BlogService;
import com.study.blog.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 10652
 */
@Component
@Slf4j
public class FlushESUtil {
    private static BlogService blogService;
    private static UserService userService;
    private static EsBlogRepository esBlogRepository;
    private final EsBlogRepository EsBlogRepositoryTemp;
    private final BlogService serviceTemp;
    private final UserService userServiceTemp;


    @Autowired
    public FlushESUtil(EsBlogRepository esBlogRepository, BlogService blogService, UserService userService) {
        this.EsBlogRepositoryTemp = esBlogRepository;
        this.serviceTemp = blogService;
        this.userServiceTemp = userService;
    }

    /**
     * 系统启动时将 数据库中的 博客 刷新到 ES 中
     */
    public static void flushESByMySQL() {
        List<Blog> blogList = blogService.listAllBlog();
        List<EsBlog> esBlogList = blogList.stream().map((blog -> {
            User user = userService.searchById(blog.getUserId());
            return new EsBlog(blog, user);
        })).collect(Collectors.toList());
        esBlogRepository.saveAll(esBlogList);
    }

    @PostConstruct
    public void init() {
        blogService = serviceTemp;
        userService = userServiceTemp;
        esBlogRepository = EsBlogRepositoryTemp;
    }

}
