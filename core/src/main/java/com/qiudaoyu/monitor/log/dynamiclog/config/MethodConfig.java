package com.qiudaoyu.monitor.log.dynamiclog.config;

/**
 * 创建时间: 2018/9/18
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class MethodConfig {

    private final static int BEFORE_MASK = 0x1;
    private final static int AFTER_MASK = 0x2;
    private final static int EXCEPTION_MASK = 0x4;
    private int beforeExceptionAfter;
    private String luaString;
    private String eventType;


    public static String getLuaRootString() {
        return "stacks={};";
    }

    public String getLuaString() {
        return luaString;
    }

    public void setLuaString(String luaString) {
        this.luaString = luaString;
    }


    public boolean isAfter() {

        return (beforeExceptionAfter & AFTER_MASK) > 0;
    }

    public boolean isException() {
        return (beforeExceptionAfter & EXCEPTION_MASK) > 0;
    }

    public boolean isBefore() {
        return (beforeExceptionAfter & BEFORE_MASK) > 0;
    }

}
