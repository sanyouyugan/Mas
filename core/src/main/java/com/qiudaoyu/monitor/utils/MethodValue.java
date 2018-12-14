package com.qiudaoyu.monitor.utils;

import java.util.Stack;

/**
 * 创建时间: 2018/12/6
 * 类描述:
 * <p>
 * 方法内有效的值
 * <p>
 * 需要在方法开始     调用methodEnter
 * <p>
 * 需要在方法结束/异常 调用methodExit
 * <p>
 * methodEnter/methodExit 需要对称调用
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class MethodValue {

    private static ThreadLocal<Stack<Object>> locals = new ThreadLocal<>();

    public static void methodEnter() {
        if (locals.get() == null) {
            locals.set(new Stack<>());
        }
        locals.get().push(null);
    }

    public static Object getValue() {
        if (locals.get() == null) {
            locals.set(new Stack<>());
            locals.get().push(null);
        }
        return locals.get().peek();
    }

    public static void setValue(Object data) {
        if (locals.get() == null) {
            locals.set(new Stack<>());
            locals.get().push(null);
        }
        locals.get().pop();
        locals.get().push(data);
    }

    public static void methodExit() {
        if (locals.get() == null) {
            locals.set(new Stack<>());
            locals.get().push(null);
        }
        locals.get().pop();
    }

    public static void clear() {
        locals.set(null);
    }
}
