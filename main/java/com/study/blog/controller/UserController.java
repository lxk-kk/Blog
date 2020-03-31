package com.study.blog.controller;

import com.github.pagehelper.Page;
import com.study.blog.entity.Authority;
import com.study.blog.entity.User;
import com.study.blog.exception.BeanValidationExceptionHandler;
import com.study.blog.service.AuthorityService;
import com.study.blog.service.impl.UserServiceImpl;
import com.study.blog.util.EntityTransfer;
import com.study.blog.util.PasswordValidation;
import com.study.blog.vo.ResultVO;
import com.study.blog.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.expression.Strings;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author 10652
 * 【spring security】
 * 在类上使用PreAuthorize注解：表示指定角色权限才能访问方法
 * PreAuthorize("hasAuthority('ADMIN')")
 * 在方法上添加这个注解：表示只有指定的用户名才能访问该方法
 * PreAuthorize("authentication.name.equals(#username)")
 */
@RestController
@RequestMapping("/users")
@Slf4j
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class UserController {
    private final UserServiceImpl service;
    private final AuthorityService authorityService;

    /**
     * fileServerUrl会在配置文件（application.yml）中进行配置，并在此处获取
     */
    // @Value("${file.server.url}")
    private String fileServerUrl;

    @Autowired
    public UserController(UserServiceImpl service, AuthorityService authorityService) {
        this.authorityService = authorityService;
        this.service = service;
    }

    /**
     * 查询所有用户
     *
     * @param model model
     * @return 用户列表页面
     */
    @GetMapping
    public ModelAndView listUser(
            @RequestParam(value = "async", required = false) boolean async,
            @RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "name", required = false, defaultValue = "") String name,
            Model model,
            HttpServletRequest request) {

        Page<User> users = service.listUsersByName(name, pageIndex, pageSize);
        model.addAttribute("page", "");
        // 将 users 转换为 userVOs
        Page<UserVO> userVOS = EntityTransfer.usersToVOS(users);
        log.info("获取用户列表：【Page<UserVO>：{}】", userVOS);
        model.addAttribute("users", userVOS);
        Strings strings = new Strings(Locale.CHINA);
        request.setAttribute("strings", strings);
        /*
            延迟加载为真？
                #mainContaineRepleace？？？？
         */
        return new ModelAndView(async ? "users/list :: #mainContainerRepleace" : "users/list", "userModel", model);
    }

    /**
     * 查询指定id的用户
     *
     * @param id    id
     * @param model model
     * @return 用户详情页面
     */
    @GetMapping("/{id}")
    ModelAndView searchById(@PathVariable("id") int id, Model model) {
        // 将 user 转换为 userVO 前端展示
        model.addAttribute("user", EntityTransfer.userToVO(service.searchById(id)));
        model.addAttribute("title", "查询用户");
        return new ModelAndView("users/view", "userModel", model);
    }

    /**
     * 增加用户
     *
     * @param model model
     * @return 具有form表单的用户信息填写页面
     */
    @GetMapping("/add")
    ModelAndView createUser(Model model) {
        model.addAttribute("user", new User());
        return new ModelAndView("users/add", "userModel", model);
    }

    /**
     * 修改指定id的用户
     *
     * @param id    id
     * @param model model
     * @return 具有form表单的用户信息填写页面
     */
    @GetMapping("/edit/{id}")
    ModelAndView updateUser(@PathVariable("id") int id, Model model) {
        model.addAttribute("user", service.searchById(id));
        return new ModelAndView("users/edit", "userModel", model);
    }

    /**
     * 保存或者更新用户
     * <p>
     * 参数不需要添加注解？
     *
     * @param user 用户
     * @return 重定向到用户列表页面
     */
    @PostMapping
    ResponseEntity<ResultVO> saveOrUpdate(Integer authorityId, @Validated User user, BindingResult result) {
        if (result.hasErrors()) {
            log.error("用户信息出错");
            return ResponseEntity.ok().body(new ResultVO(false, result.getFieldError().getDefaultMessage()));
        }

        List<Authority> authorities = new ArrayList<>(1);
        authorities.add(authorityService.getAuthorityById(authorityId));
        user.setAuthorities(authorities);
        User originalUser;
        if (user.getId() != null && (originalUser = service.searchById(user.getId())) != null) {
            // 用户信息更新
            /*
                这里需要对密码进行等值判断：原因是
                    保存的密码是加密后的密码
                    如果用户修改了密码，则新密码是未加密的，所以需要进行判断
             */
            if (!PasswordValidation.equalPassword(originalUser.getPassword(), user.getPassword())) {
                log.info("更新密码了：{}，{}", originalUser.getPassword(), user.getPassword());
                // 如果密码被更新过，则需要对其进行加密保存
                user.setEncodePassword(user.getPassword());
            }
            // 否则不需要更新密码
            try {
                service.updateUser(user);
            } catch (ConstraintViolationException e) {
                String errorMsg = BeanValidationExceptionHandler.getExceptionMessage(e);
                log.error("【更新用户信息 ：Bean 校验异常】{}", errorMsg);
                return ResponseEntity.ok().body(new ResultVO(false, errorMsg));
            }

        } else {
            // 新增用户
            user.setEncodePassword(user.getPassword());
            try {
                service.createUser(user);
            } catch (ConstraintViolationException e) {
                String errorMsg = BeanValidationExceptionHandler.getExceptionMessage(e);
                log.error("【新增用户 ：Bean 校验异常】{}", errorMsg);
                return ResponseEntity.ok().body(new ResultVO(false, errorMsg));
            }
        }
        return ResponseEntity.ok().body(new ResultVO(true, "处理成功", user));
    }

    /**
     * 删除指定id的用户
     *
     * @param id id
     * @return 重定向到用户列表
     */
    @DeleteMapping("/delete/{id}")
    ResponseEntity<ResultVO> deleteUser(@PathVariable("id") int id) {
        try {
            service.deleteUser(id);
        } catch (Exception e) {
            return ResponseEntity.ok().body(new ResultVO(false, e.getMessage()));
        }
        return ResponseEntity.ok().body(new ResultVO(true, "处理成功"));
    }

    /**
     * 获取个人设置页面
     *
     * @param username     用户名
     * @param modelAndView 模型视图
     * @return 修改个人信息界面
     */
    @GetMapping("/{username}/profile")
    @PreAuthorize("authentication.name.equals(#username)")
    public ModelAndView profile(@PathVariable("username") String username, ModelAndView modelAndView) {
        User user = (User) service.loadUserByUsername(username);
        modelAndView.addObject("user", user);
        // 将文件服务器的地址返回给客户端：通过该文件服务器的地址查询到用户的头像的具体位置
        modelAndView.addObject("fileServerUrl", fileServerUrl);
        modelAndView.setViewName("/userspace/profile");
        modelAndView.setViewName("userModel");
        return modelAndView;
    }

    /**
     * @param username 用户名
     * @param user     用户
     * @return 用户个人信息页面
     */
    @PostMapping("/{username}/profile")
    @PreAuthorize("authentication.name.equals(#username)")
    public String saveProfile(@PathVariable("username") String username, User user) {
        log.info("修改用户信息");

        User originUser = service.findOneByUsername(username);
        originUser.setEmail(user.getEmail());
        originUser.setName(user.getName());
        if (!PasswordValidation.equalPassword(originUser.getPassword(), user.getPassword())) {
            originUser.setEncodePassword(user.getPassword());
        }
        service.updateUser(originUser);
        return "redirect:/u/" + username + "/profile";
    }

    /**
     * 获取修改用户头像数据 到达用户编辑头像的页面
     *
     * @param username     用户名
     * @param modelAndView 模型视图
     * @return 编辑头像页面
     */
    @GetMapping("/{username}/avatar")
    @PreAuthorize("authentication.name.equals(#username)")
    public ModelAndView avatar(@PathVariable("username") String username, ModelAndView modelAndView) {
        User originUser = service.findOneByUsername(username);
        modelAndView.addObject("user", originUser);
        modelAndView.setViewName("/userspace/avatar");
        modelAndView.setViewName("userModel");
        return modelAndView;
    }

    /**
     * 保存用户头像，响应新的头像地址
     *
     * @param username 用户名
     * @param user     用户
     * @return 处理结果
     */
    @PostMapping("/{username}/avatar")
    @PreAuthorize("authentication.name.equals(#username)")
    public ResponseEntity<ResultVO> avatar(@PathVariable("username") String username, @RequestBody User user) {
        String avatar = user.getAvatar();
        User originUser = service.findOneByUsername(username);
        originUser.setAvatar(avatar);
        service.updateUser(originUser);
        return ResponseEntity.ok().body(new ResultVO(true, "处理成功", avatar));
    }
}
