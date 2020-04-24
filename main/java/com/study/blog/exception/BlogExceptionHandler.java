package com.study.blog.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author 10652
 */
@Slf4j
@ControllerAdvice
public class BlogExceptionHandler {
    @ExceptionHandler({LimitFlowException.class, NullBlogException.class})
    public String modelAndViewRequestException(Exception e, Model model) {
        log.error("ErrorMsg：{}", e.getMessage());
        model.addAttribute("message", e.getMessage());
        return "/errorPage";
    }

}
