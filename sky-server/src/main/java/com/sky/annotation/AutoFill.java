package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author zhangpj
 * @date 2026/2/6
 */
// 指定注解所表示的拦截方法需要填充的字段
@Target(ElementType.METHOD)
// 指定注解所表示的拦截方法所应用的时间点
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    // 设置所拦截的方法所对应的操作类型
    OperationType value();
}
