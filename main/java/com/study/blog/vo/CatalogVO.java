package com.study.blog.vo;

import com.study.blog.entity.Catalog;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class CatalogVO implements Serializable {
    private static final long serialVersionUID = -7936086599700202861L;

    /**
     * 用户账号
     */
    private String username;
    /**
     * 分类
     */
    @NotNull(message = "请填写分类")
    private Catalog catalog;

}
