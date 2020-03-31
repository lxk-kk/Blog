package com.study.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author 10652
 */
@Getter
@Setter
@AllArgsConstructor
public class Menu implements Serializable{

    private static final long serialVersionUID = 5831918023266221185L;
    private String name;
    private String url;
}

