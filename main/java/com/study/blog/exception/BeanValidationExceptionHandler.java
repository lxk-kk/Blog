package com.study.blog.exception;

import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * 对bean验证出现的异常进行统一的处理
 * 由于bean验证时可能会抛出多个异常，所以这里接收的是 ConstraintViolationException
 * @author 10652
 */
public class BeanValidationExceptionHandler {
    public static String getExceptionMessage(ConstraintViolationException e){
        List<String> msgList=new ArrayList<>(1);
        for (ConstraintViolation constraintViolation:e.getConstraintViolations()){
            msgList.add(constraintViolation.getMessage());
        }
        String msg= StringUtils.collectionToDelimitedString(msgList,";");
        return msg;
    }
}
