package com.study.blog.controller;

import com.study.blog.annotation.ValidateAnnotation;
import com.study.blog.vo.Menu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

/**
 * 后台管理控制器：管理用户
 *
 * @author 10652
 */
@Slf4j
@Controller
@RequestMapping("/admins")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {
    /**
     * 返回用户列表页面
     *
     * @param model model
     * @return 后台管理的主页
     */
    @GetMapping
    @ValidateAnnotation
    public ModelAndView listUsers(Model model) {
        List<Menu> list = new ArrayList(1);
        list.add(new Menu("用户管理", "/users"));
        model.addAttribute("list", list);
        return new ModelAndView("admins/index", "menuList", model);
    }
}
