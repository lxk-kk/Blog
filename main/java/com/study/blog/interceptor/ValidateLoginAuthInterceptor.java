package com.study.blog.interceptor;

import com.study.blog.annotation.ValidateAnnotation;
import com.study.blog.constant.ValidateConstant;
import com.study.blog.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author 10652
 * <p>
 * 新权限认证方式：拦截所有被标记的方法，验证登录 与 权限信息
 */
@Slf4j
public class ValidateLoginAuthInterceptor implements HandlerInterceptor {

    @Value("${validate.start}")
    private static String LET_GO;

    /**
     * 拦截方法：验证登录、权限信息
     *
     * @param request  request
     * @param response response
     * @param handler  处理器
     * @return 通过与否
     * @throws Exception e
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws
            Exception {
        // log.info("Request：{}", request.getRequestURI());
        // 新的登录认证还未完全重构完成，所以，现在就都放行：日后再开启！
        if (Objects.equals("yes", ValidateConstant.LET_GO_YES)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (Objects.isNull(session)) {
            log.info("未登录");
            // todo 抛出异常
            return false;
        }
        if (!(handler instanceof HandlerMethod)) {
            log.error("handler 错误");
            // todo 抛出异常
            return false;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 获取方法 对象
        Method method = handlerMethod.getMethod();
        // 获取方法 上的 ValidateAnnotation 注解
        ValidateAnnotation validate = method.getAnnotation(ValidateAnnotation.class);
        if (Objects.isNull(validate)) {
            // 说明不需要管理员的权限：放行
            return true;
        }
        User user = null;
        try {
            user = (User) session.getAttribute(ValidateConstant.USER_INFO);
        } catch (ClassCastException e) {
            log.error("登录异常，需重新登录");
            // todo 抛出异常
            return false;
        }
        int authorityId = user.getAuthorities().get(0).getAuthorityId();
        // 判断用户权限 与 方法所要求的权限是否匹配
        if (authorityId == 0 || authorityId > validate.authorityId()) {
            log.info("没有权限");
            // todo 抛出异常
            return false;
        }
        // 最后说明是管理员：成功放行
        return true;
    }
}
