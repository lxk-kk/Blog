package com.study.blog.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 10652
 */
@Slf4j
@Aspect
@Component
public class FileServerAspect {
    @Before("execution( public * com.study.blog.controller.*.*(..))")
    public void fileServerBefore() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        log.info("file server request info：{}", request.getRequestURI());
    }
}
