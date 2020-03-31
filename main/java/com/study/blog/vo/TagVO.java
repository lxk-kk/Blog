package com.study.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author 10652
 */
@Data
@AllArgsConstructor
public class TagVO implements Serializable{
    private static final long serialVersionUID = -6465974689882079123L;
    private String name;
    private Long count;
}
