package com.qiudaoyu.monitor.log.mlog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 创建时间: 2018/10/9
 * 类描述:设置当前方法产生的事件的父节点
 *
 * @author 秋刀鱼
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface ParentId {
    String parentId() default "";
}
