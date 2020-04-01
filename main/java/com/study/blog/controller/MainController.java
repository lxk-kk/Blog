package com.study.blog.controller;

import com.study.blog.constant.ValidateConstant;
import com.study.blog.entity.Authority;
import com.study.blog.entity.LoginEntity;
import com.study.blog.entity.User;
import com.study.blog.service.AuthorityService;
import com.study.blog.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author 10652
 * 该controller用于接收 主页请求、登录请求
 */
@Controller
@Slf4j
public class MainController {

    private static final Integer ROLE_USER_AUTHORITY_ID = 2;

    private final AuthorityService authorityService;
    private final UserService userService;

    @Autowired
    public MainController(AuthorityService authorityService, UserService userService) {
        this.authorityService = authorityService;
        this.userService = userService;
    }

    /**
     * 请求根目录时：重定向到主页的请求
     *
     * @return 重定向
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/index";
    }

    /**
     * 访问首页
     * 重定向到 blogs 请求，加载所有的全文索引数据，将所有数据绑定到index页面
     *
     * @return 首页
     */
    @GetMapping("/index")
    public String index() {
        return "redirect:/blogs";
    }

    @GetMapping("/login")
    public String login() {
        log.info("登录！");
        return "/login";
    }

    /**
     * 新的登录认证方式：Spring Session
     *
     * @param loginEntity 登录实体
     * @param result      校验
     * @param request     request
     * @return 主页
     */
    @PostMapping("/login_new")
    public String login(@RequestBody @Validated LoginEntity loginEntity,
                        BindingResult result, HttpServletRequest request) {
        if (result.hasErrors()) {
            log.error("字段验证：{}", result.getFieldError().getDefaultMessage());
            // todo 返回错误提示信息
        }
        User user = userService.validateUser(loginEntity.getUsername(), loginEntity.getPassword());
        if (Objects.isNull(user)) {
            log.error("用户验证错误");
            // todo 返回错误提示信息
        }
        log.info("【login  JSESSIONID】={}", request.getSession(true).getId());
        HttpSession session = request.getSession();
        session.setAttribute(ValidateConstant.USER_INFO, user);
        return "/index";
    }

    @GetMapping("/loginError")
    public ModelAndView failure(Model model) {
        model.addAttribute("loginError", true);
        model.addAttribute("errorMsg", "用户名或密码错误");
        return new ModelAndView("/login", "model", model);
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "/register";
    }

    @PostMapping("/register")
    public String register(@Validated User user, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("msg", bindingResult.getFieldError().getDefaultMessage());
            return "/register";
        }
        List<Authority> authorities = new ArrayList<>(1);
        authorities.add(authorityService.getAuthorityById(ROLE_USER_AUTHORITY_ID));
        user.setAuthorities(authorities);
        // 将 password 加密处理
        user.setEncodePassword(user.getPassword());
        userService.createUser(user);
        return "redirect:login";
    }
}


