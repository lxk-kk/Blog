package com.study.blog.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author 10652
 */
@Data
@NoArgsConstructor
public class Catalog implements Serializable {
    private static final long serialVersionUID = 6753848220249930315L;
    Long id;
    @NotBlank(message = "请填写分类")
    String name;

    Integer userId;

    public Catalog(String name, Integer userId) {
        this.name = name;
        this.userId = userId;
    }
}
