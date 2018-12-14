package com.qiudaoyu.monitor.utils;

/**
 * 创建时间: 2018/12/11
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public interface ContentLisenter {
    void content(String content);

    void error(Exception e,String s);
}
