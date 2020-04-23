package com.study.blog.controller;

import com.study.blog.entity.EsBlog;
import com.study.blog.entity.User;
import com.study.blog.service.EsBlogService;
import com.study.blog.vo.TagVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

/**
 * @author 10652
 */
@Controller
@Slf4j
@RequestMapping("/blogs")
public class BlogController {

    private final EsBlogService esBlogService;

    @Autowired
    public BlogController(EsBlogService service) {
        this.esBlogService = service;
    }

    @GetMapping
    public String listEsBlogs(
            @RequestParam(value = "order", required = false, defaultValue = "new") String order,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "async", required = false) boolean async,
            @RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
            Model model) {
        Page<EsBlog> page = null;
        List<EsBlog> blogList;
        // 系统初始化时，没有博客数据
        boolean isEmpty = true;
        log.info("keyword:{}", keyword);
        log.info("pageIndex:{}", pageIndex);
        log.info("pageSize:{}", pageSize);
        log.info("order:{}", order);
        try {
            if (Objects.equals(order, "hot")) {
                // 最热查询
                Sort sort = new Sort(Sort.Direction.DESC, "readCount", "commentCount", "likeCount", "createTime");
                Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);
                page = esBlogService.listHotestEsBlog(keyword, pageable);
            } else if (Objects.equals(order, "new")) {
                // 最新查询
                Sort sort = new Sort(Sort.Direction.DESC, "createTime");
                Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);
                page = esBlogService.listNewestEsBlog(keyword, pageable);
            }
            isEmpty = false;
        } catch (Exception e) {
            Pageable pageable = PageRequest.of(pageIndex, pageSize);
            page = esBlogService.listEsBlog(pageable);
        }

        // 当前所在页面数据列表
        blogList = page.getContent();

        model.addAttribute("order", order);
        model.addAttribute("keyword", keyword);
        model.addAttribute("page", page);
        model.addAttribute("blogList", blogList);

        // 首次访问页面后才会加载
        /*
        这里做判断是为了避免重复加载一下全文索引的数据
        当用户第一次访问的时候就加载，此后不会加载：例如，分页时，点击下一页时，点击搜索时等待一下数据就不会重新加载
        默认为，一下数据在短时间内不会发生变化
         */
        if (!async && !isEmpty) {

        }
        List<EsBlog> newest = esBlogService.listTop5NewestEsBlog();
        model.addAttribute("newest", newest);
        List<EsBlog> hotest = esBlogService.listTop5HotestEsBlog();
        model.addAttribute("hotest", hotest);
        List<TagVO> tags = esBlogService.listTop30Tag();
        model.addAttribute("tags", tags);
        List<User> users = esBlogService.listTop12User();
        model.addAttribute("users", users);
        return (async ? "/index :: #mainContainerRepleace" : "/index");

    }

    @GetMapping("/newest")
    @Deprecated
    public String listNewestEsBlog(Model model) {
        List<EsBlog> newest = esBlogService.listTop5NewestEsBlog();
        model.addAttribute("newest", newest);
        return "newest";
    }

    @GetMapping("/hotest")
    @Deprecated
    public String listHotestEsBlog(Model model) {
        List<EsBlog> hotest = esBlogService.listTop5HotestEsBlog();
        model.addAttribute("hotest", hotest);
        return "hotest";
    }
}
