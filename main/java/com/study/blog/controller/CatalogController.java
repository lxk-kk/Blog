package com.study.blog.controller;

import com.study.blog.annotation.ValidateAnnotation;
import com.study.blog.entity.Catalog;
import com.study.blog.entity.User;
import com.study.blog.service.CatalogService;
import com.study.blog.service.UserService;
import com.study.blog.vo.CatalogVO;
import com.study.blog.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * @author 10652
 */
@Slf4j
@Controller
@RequestMapping("/catalogs")
public class CatalogController {

    private final UserService userService;
    private final CatalogService catalogService;

    @Autowired
    public CatalogController(UserService userService, CatalogService catalogService) {
        this.userService = userService;
        this.catalogService = catalogService;
    }

    /**
     * 获取分类列表
     *
     * @param username 用户名
     * @param model    model
     * @return 分类列表
     */
    @GetMapping
    public String listCatalog(@RequestParam("username") String username, Model model) {
        // 根据用户名获取用户
        User user = userService.findOneByUsername(username);
        assert user != null;
        log.info("blog user id={}", user.getId());
        List<Catalog> catalogList = catalogService.listCatalogs(user.getId());

        // 判断操作用户是否为分类的所有者？
        boolean isOwner = false;

        // todo 这个判断：博主用户==当前登录验证过的用户 ？
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !Objects.equals(authentication.getPrincipal().toString(), "anonymousUser")) {
            User principal = (User) authentication.getPrincipal();

            isOwner = principal != null && Objects.equals(principal.getName(), user.getName());
        }

        model.addAttribute("isCatalogsOwner", isOwner);
        model.addAttribute("catalogs", catalogList);
        return "/userspace/u::#catalogRepleace";
    }

    /**
     * 发表分类
     *
     * @param catalogVO 分类
     * @return 处理结果
     * <p>
     * 通过POST请求创建分类，并且验证当前登录用户是否为分类用户，如果是，则才能进行后续操作，否则将会被拦截住
     */
    @PostMapping
    @PreAuthorize("authentication.name.equals(#catalogVO.username)")
    @ValidateAnnotation(authorityId = 2)
    public ResponseEntity<ResultVO> create(@RequestBody @Validated CatalogVO catalogVO, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.ok().body(new ResultVO(false,result.getFieldError().getDefaultMessage()));
        }
        String username = catalogVO.getUsername();
        Catalog catalog = catalogVO.getCatalog();
        if (StringUtils.isEmpty(catalog.getName().trim())) {
            return ResponseEntity.ok().body(new ResultVO(false, "请填写分类！"));
        }
        User user = userService.findOneByUsername(username);
        try {
            catalog.setUserId(user.getId());
            if (catalog.getId() != null) {
                // 更新分类：保存
                catalogService.updateCatalog(catalog);
            } else {
                // 新建分类：保存
                catalogService.saveCatalog(catalog);
            }
        } catch (Exception e) {
            return ResponseEntity.ok().body(new ResultVO(false, e.getMessage()));
        }
        return ResponseEntity.ok().body(new ResultVO(true, "发布成功"));
    }

    /**
     * 删除分类：进行验证：只有本人才能有权限删除分类
     *
     * @param username 分类博主账号？
     * @param id       分类id
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("authentication.name.equals(#username)")
    @ValidateAnnotation(authorityId = 2)
    public ResponseEntity<ResultVO> delete(String username, @PathVariable("id") Long id) {
        try {
            catalogService.removeCatalog(id);
        } catch (Exception e) {
            return ResponseEntity.ok().body(new ResultVO(false, e.getMessage()));
        }
        return ResponseEntity.ok().body(new ResultVO(true, "处理成功"));
    }

    /**
     * 获取新增分类编辑页面：new 一个 catalog 绑定到模型中返回到页面
     *
     * @param model model
     * @return 编辑页面
     */
    @GetMapping("/edit")
    @ValidateAnnotation(authorityId = 2)
    public String getCatalogEdit(Model model) {
        log.info("跳转分类编辑页面");
        Catalog catalog = new Catalog();
        model.addAttribute("catalog", catalog);
        return "/userspace/catalogedit";
    }

    /**
     * 根据 分类id 编辑分类：需要通过id查询到相应的Catalog对象，并切绑定到相应的模型中
     *
     * @param id    分类id
     * @param model model
     * @return 编辑页面
     */
    @GetMapping("/edit/{id}")
    @ValidateAnnotation(authorityId = 2)
    public String getCatalogById(@PathVariable("id") Long id, Model model) {
        Catalog catalog = catalogService.getCatalogById(id);
        model.addAttribute("catalog", catalog);
        return "/userspace/catalogedit";
    }

}
