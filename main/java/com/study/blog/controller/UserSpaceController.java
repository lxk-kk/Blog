package com.study.blog.controller;

import com.github.pagehelper.Page;
import com.study.blog.annotation.ValidateAnnotation;
import com.study.blog.entity.Blog;
import com.study.blog.entity.Catalog;
import com.study.blog.entity.User;
import com.study.blog.service.BlogService;
import com.study.blog.service.CatalogService;
import com.study.blog.service.VoteService;
import com.study.blog.service.impl.UserServiceImpl;
import com.study.blog.util.PasswordValidation;
import com.study.blog.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户主页
 *
 * @author 10652
 */
@Controller
@RequestMapping("/u")
@Slf4j
public class UserSpaceController {

    private static final String ORDER_NEW = "new";
    private static final String ORDER_HOT = "hot";
    private final BlogService blogService;
    private final VoteService voteService;
    private final UserServiceImpl userService;
    private final CatalogService catalogService;

    @Autowired
    public UserSpaceController(BlogService blogService, VoteService voteService, UserServiceImpl userService,
                               CatalogService catalogService) {
        this.blogService = blogService;
        this.voteService = voteService;
        this.userService = userService;
        this.catalogService = catalogService;
    }

    /**
     * 根据用户名称查询用户主页
     *
     * @param username 用户名
     * @return 用户主页（返回到网页）
     */
    @GetMapping("/{username}")
    @ValidateAnnotation(authorityId = 2)
    public String userSpace(@PathVariable("username") String username, Model model) {
        User user = (User) userService.loadUserByUsername(username);
        // 这个user 属性应该没用，下面是 redirect
        model.addAttribute("user", user);
        return "redirect:/u/" + username + "/blogs";
    }

    /**
     * 访问某个用户的博客主页：只有指定用户名的用户才能访问该方法
     *
     * @param username 用户名
     * @param model    模型视图
     * @return 模型视图
     */
    @GetMapping("/{username}/profile")
    @ValidateAnnotation(authorityId = 2)
    @PreAuthorize("authentication.name.equals(#username)")
    public ModelAndView profile(@PathVariable("username") String username, Model model, RedirectAttributes attributes) {
        User user = (User) userService.loadUserByUsername(username);
        log.info("【user 主页】：{}", user);
        Map<String, Objects> map = (Map<String, Objects>) attributes.getFlashAttributes();
        Objects msg = map.get("msg");
        log.info("msg:{}", msg);
        model.addAttribute("msg", map.get("msg"));
        model.addAttribute("user", user);
        return new ModelAndView("userspace/profile", "userModel", model);
    }

    /**
     * 保存某个用户的个人信息：只有指定用户名的用户才能访问该方法
     *
     * @param username 用户名
     * @param user     用户
     * @return 重定向到该用户的博客主页
     */
    @PostMapping("/{username}/profile")
    @PreAuthorize("authentication.name.equals(#username)")
    @ValidateAnnotation(authorityId = 2)
    public String saveProfile(@PathVariable("username") String username, @Validated User user, BindingResult result,
                              ModelAndView model) {

        if (result.hasErrors()) {
            RedirectAttributes attributes = new RedirectAttributesModelMap();
            attributes.addFlashAttribute("msg", result.getFieldError().getDefaultMessage());
            return "redirect:/u/" + username + "/profile";
        }
        User origin = userService.searchByUsername(username);
        origin.setEmail(user.getEmail());
        // 验证密码是否被修改过，如果被修改过则需要对其进行加密保存
        if (!PasswordValidation.equalPassword(origin.getPassword(), user.getPassword())) {
            user.setEncodePassword(user.getPassword());
        }
        userService.updateUser(user);
        return "redirect:/u/" + username + "/profile";
    }

    /**
     * 获取编辑用户头像的界面：指定用户名的用户才能访问
     *
     * @param username 用户名
     * @param model    模型
     * @return 视图模型
     */
    @GetMapping("/{username}/avatar")
    @PreAuthorize("authentication.name.equals(#username)")
    @ValidateAnnotation(authorityId = 2)
    public ModelAndView avatar(@PathVariable("username") String username, Model model) {
        User user = userService.searchByUsername(username);
        model.addAttribute("user", user);
        return new ModelAndView("userspace/avatar", "userModel", model);
    }

    /**
     * 保存用户头像：只有指定用户名的用户才能访问该方法
     *
     * @param username 用户名
     * @param user     用户
     * @return 处理结果
     */
    @PostMapping("/{username}/avatar")
    @PreAuthorize("authentication.name.equals(#username)")
    @ValidateAnnotation(authorityId = 2)
    public ResponseEntity<ResultVO> avatar(@PathVariable("username") String username, @RequestBody User user) {
        log.info("user:{}", user);
        userService.updateUserAvatar(username, user.getAvatar());
        return ResponseEntity.ok(new ResultVO(true, "头像更新成功", user.getAvatar()));
    }

    /**
     * 根据用户名称查询用户博客：三选一的查询方式：最终返回到博客列表页面
     *
     * @param username  用户名
     * @param order     按照博客顺序查询
     * @param catalogId 按照博客类目查询
     * @param keyword   按照关键字查询
     * @return 博客列表页
     */
    @GetMapping("/{username}/blogs")
    @ValidateAnnotation(authorityId = 2)
    public String listBlogsByOrder(@PathVariable("username") String username,
                                   @RequestParam(value = "order", required = false, defaultValue = "new") String order,
                                   @RequestParam(value = "catalog", required = false) Long catalogId,
                                   @RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "async", required = false, defaultValue = "false") boolean
                                           async,
                                   @RequestParam(value = "pageIndex", required = false, defaultValue = "1") int
                                           pageIndex,
                                   @RequestParam(value = "pageSize", required = false, defaultValue = "10") int
                                           pageSize,
                                   Model model) {

        User user = (User) userService.loadUserByUsername(username);
        Page<Blog> blogs = null;
        if (!Objects.isNull(catalogId) && catalogId > 0) {
            log.info("【分类查询】 catalogId{}", catalogId);
            blogs = blogService.listBlogByCatalog(catalogId, pageIndex, pageSize);
            order = "";
        }
        if (order.equals(ORDER_NEW)) {
            log.info("【最新查询】 keyword:{}", keyword);
            // 最新查询
            blogs = blogService.listBlogsByTitleLikeAndDescSort(user.getId(), keyword, pageIndex, pageSize);
        } else if (order.equals(ORDER_HOT)) {
            log.info("【最热查询】 keyword:{}", keyword);
            // 最热查询
            blogs = blogService.listBlogsByTitleLike(user.getId(), keyword, pageIndex, pageSize);
        }

        model.addAttribute("user", user);
        model.addAttribute("order", order);
        model.addAttribute("blogList", blogs);
        // todo 为什么将 catalogId 和 keyword 一起返回给前端？
        model.addAttribute("catalogId", catalogId);
        model.addAttribute("keyword", keyword);
        /*
        * 根据 传入的字段 async 判断是否异步加载？
        * */
        return async ? "/userspace/u::#mainContainerRepleace" : "/userspace/u";
    }

    /**
     * 根据博客id查询
     *
     * @param username 用户名
     * @param id       博客id
     * @return 具体的博客页面
     */
    @GetMapping("/{username}/blogs/{id}")
    @ValidateAnnotation(authorityId = 2)
    public String getBlogById(@PathVariable("username") String username, @PathVariable("id") Long id, Model
            model) {
        // 每次查询阅读量增加一次：较为简易
        blogService.readingIncrement(id);

        User principal = null;
        boolean isBlogOwner = false;

        // 判断操作用户是否是博客的所有者：根据登录用户的 principal 的用户名判断
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            principal = (User) authentication.getPrincipal();
            if (principal != null && username.equals(principal.getUsername())) {
                isBlogOwner = true;
            }
        }
        // log.info("principal:{}", principal)
        Long voteId;
        if (Objects.isNull(principal)) {
            voteId = 0L;
        } else {
            // 用户都已经认证过了！
            voteId = voteService.isVoted(id, principal.getId());
        }
        // log.info("voteId:{}", voteId);
        // 将 是否为本人 判断结果返回给前端

        // long time = System.currentTimeMillis();
        Blog blog = blogService.getBlogById(id);
        // System.out.println("getBlog 总耗时：" + (System.currentTimeMillis() - time));

        // log.info("blog:{}", blog);
        // 评论量、点赞量的设置：应该在 评论（点赞）有改动的时候设置
        model.addAttribute("isBlogOwner", isBlogOwner);
        model.addAttribute("blogModel", blog);
        model.addAttribute("blogEditor", userService.searchById(blog.getUserId()));
        model.addAttribute("voteId", voteId);
        // log.info("8/9 总耗时：{},Blog：{}", System.currentTimeMillis() - timeSum, blog.getBlogId());
        return "userspace/blog";
    }

    /**
     * 删除博客
     *
     * @param username 用户名
     * @param id       博客id
     * @return 重定向
     */
    @DeleteMapping("/{username}/blogs/{id}")
    @PreAuthorize("authentication.name.equals(#username)")
    @ValidateAnnotation(authorityId = 2)
    public ResponseEntity removeBlog(@PathVariable("username") String username, @PathVariable("id") Long id) {
        try {
            blogService.removeBlog(id);
        } catch (Throwable throwable) {
            return ResponseEntity.ok().body(new ResultVO(false, throwable.getMessage()));
        }
        String redirectUrl = "/u/" + username + "/blogs";
        return ResponseEntity.ok().body(new ResultVO(true, "处理成功", redirectUrl));
    }

    /**
     * 获取新增博客的界面
     *
     * @param model model
     * @return model
     */
    @GetMapping("/{username}/blogs/edit")
    @ValidateAnnotation(authorityId = 2)
    public ModelAndView createBlog(@PathVariable("username") String username, Model model) {
        // 将用户自己的分类列表返回，供用户编辑博客时使用！
        User user = (User) userService.loadUserByUsername(username);
        List<Catalog> catalogs = catalogService.listCatalogs(user.getId());
        model.addAttribute("blog", new Blog(null, null, null));
        model.addAttribute("catalogs", catalogs);
        return new ModelAndView("/userspace/blogedit", "blogModel", model);
    }

    /**
     * 获取编辑博客的界面
     *
     * @param model model
     * @return model
     */
    @GetMapping("/{username}/blogs/edit/{id}")
    @ValidateAnnotation(authorityId = 2)
    public ModelAndView createBlog(@PathVariable("username") String username, @PathVariable("id") Long id, Model
            model) {
        // 将用户自己的分类列表返回，供用户编辑博客时使用！
        User user = (User) userService.loadUserByUsername(username);
        List<Catalog> catalogs = catalogService.listCatalogs(user.getId());

        Blog blog = blogService.getBlogById(id);
        model.addAttribute("blog", blog);
        model.addAttribute("catalogs", catalogs);
        return new ModelAndView("/userspace/blogedit", "blogModel", model);
    }

    /**
     * 保存博客：（强制分类）首先对博客分类进行判空处理：如果分类为空，则需要添加分类！
     *
     * @param username 用户名
     * @param blog     博客
     * @return 博客页面：后续重定向到该博客页
     */
    @PostMapping("/{username}/blogs/edit")
    @ValidateAnnotation(authorityId = 2)
    @PreAuthorize(value = "authentication.name.equals(#username)")
    public ResponseEntity saveBlog(@PathVariable("username") String username, @RequestBody @Validated Blog blog,
                                   BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.ok().body(new ResultVO(false, result.getFieldError().getDefaultMessage()));
        }

        //分类判空处理
        if (blog.getCatalog().getId() == null) {
            log.error("【保存博客】 未选择分类");
            return ResponseEntity.ok().body(new ResultVO(false, "未选择分类"));
        }
        log.info("保存：{}", blog);
        User user = (User) userService.loadUserByUsername(username);
        if (blog.getBlogId() != null && blog.getBlogId() >= 1) {
            // 更新博客
            blogService.updateBlog(blog, user);
        } else {
            // 新建博客
            blog.setUserId(user.getId());
            try {
                blogService.saveBlog(blog, user);
            } catch (Throwable throwable) {
                return ResponseEntity.ok().body(new ResultVO(false, throwable.getMessage()));
            }
        }
        String redirectUrl = "/u/" + username + "/blogs/" + blog.getBlogId();
        return ResponseEntity.ok().body(new ResultVO(true, "处理成功", redirectUrl));
    }
}
