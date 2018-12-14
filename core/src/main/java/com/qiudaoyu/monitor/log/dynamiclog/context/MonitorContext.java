package com.qiudaoyu.monitor.log.dynamiclog.context;

import com.qiudaoyu.monitor.log.dynamiclog.pool.ObjectPool;

import org.keplerproject.luajava.LuaState;

import java.util.Stack;

/**
 * 创建时间: 2018/9/25
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class MonitorContext extends ObjectPool.RecyclableObject {
    private long threadId;
    private String threadName;
    private LuaState state;
    private boolean inited;

    private Stack<Boolean> stack;

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public LuaState getState() {
        return state;
    }

    public void setState(LuaState state) {
        this.state = state;
        if (state != null) {
            setInited(true);
            stack = new Stack();
        } else {
            setInited(false);
        }
    }

    public boolean isInited() {
        return inited;
    }

    public void setInited(boolean inited) {
        this.inited = inited;
    }


    public void popTop() {
        if (!stack.empty()) {
            stack.pop();
        }
    }

    public boolean getTopArg() {
        if (!stack.empty()) {
            return stack.firstElement().booleanValue();
        }
        return false;
    }

    public void pushTopArg(Boolean b) {
        stack.push(b);
    }

    public void realese() {
        if (state != null) {
            state.close();
        }
        setState(null);
        if (stack != null) {
            stack.clear();
        }
        threadName = null;
    }
}
