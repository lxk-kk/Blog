package com.study.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一数据返回格式
 * @author 10652
 */
@Getter
@AllArgsConstructor
public class ResultVO {
    private Boolean successful;
    private String message;
    private Object body;

    public ResultVO(Boolean successful, String message) {
        this.successful = successful;
        this.message = message;
    }
}
