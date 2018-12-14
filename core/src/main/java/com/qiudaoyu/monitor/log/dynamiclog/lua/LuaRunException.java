package com.qiudaoyu.monitor.log.dynamiclog.lua;

/**
 * 创建时间: 2018/9/25
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class LuaRunException extends Exception {
    private int javaCallLua;
    private int luaCallStack;
    private Object error;
    private Object error2;

    public LuaRunException() {
        super();
    }

    public LuaRunException(int javaCallLua, int luaCallStack, Object error, Object error2) {
        super();
        this.javaCallLua = javaCallLua;
        this.luaCallStack = javaCallLua;
        this.error = error;
        this.error2 = error2;
    }




}
