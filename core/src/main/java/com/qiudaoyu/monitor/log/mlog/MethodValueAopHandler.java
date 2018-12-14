package com.qiudaoyu.monitor.log.mlog;

import com.qiudaoyu.monitor.analysis.network.NetMonitor;
import com.qiudaoyu.monitor.utils.MethodValue;
import com.qiudaoyu.monitor.analysis.network.NetMonitor;
import com.qiudaoyu.monitor.utils.MethodValue;

import okhttp3.OkHttpClient;

/**
 * 创建时间: 2018/10/9
 * 类描述:自定义aop处理的处理类
 * <p>
 * 多线程执行同一方法
 * 通过ThreadLocal变量解决
 * <p>
 * 递归执行同一方法
 * 在同一线程中通过栈解决
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public abstract class MethodValueAopHandler implements MAopHandler {
    /**
     * 如果before有数据需要传递给after,使用ThreadLocal来实现
     *
     * @param classOrInstance
     * @param params
     * @param args
     */
    public void before(String[] params, Object classOrInstance, Object[] args) {
        MethodValue.methodEnter();
        if (params != null && params.length == 1
                && params[0].equals("okhttp3.OkHttpClient$Builder.build")
                && classOrInstance != null) {
            NetMonitor.beforeOkHttpBuild((OkHttpClient.Builder) (classOrInstance));
        }
        vBefore(params, classOrInstance, args);
    }


    /**
     * mode 1-正常返回 0-exception处退出
     *
     * @param mode
     * @param returnValue
     * @param params
     * @param classOrInstance
     * @param args
     */
    public void after(int mode, Object returnValue, String[] params, Object classOrInstance, Object[] args) {
        try {
            vAfter(mode, returnValue, params, classOrInstance, args);
        } finally {
            MethodValue.methodExit();
        }
    }

    public Object getValue() {
        return MethodValue.getValue();
    }

    public void setValue(Object value) {
        MethodValue.setValue(value);
    }

    /**
     * returnValue
     *
     * @param classOrInstance
     * @param params
     * @param args
     */
    public void exception(Object exception, String[] params, Object classOrInstance, Object[] args) {
        vException(exception, params, classOrInstance, args);
    }

    /**
     * 如果before有数据需要传递给after,使用ThreadLocal来实现
     *
     * @param classOrInstance
     * @param params
     * @param args
     */
    public abstract void vBefore(String[] params, Object classOrInstance, Object[] args);


    /**
     * mode 1-正常返回 0-exception处退出
     *
     * @param mode
     * @param returnValue
     * @param params
     * @param classOrInstance
     * @param args
     */
    public abstract void vAfter(int mode, Object returnValue, String[] params, Object classOrInstance, Object[] args);

    /**
     * returnValue
     *
     * @param classOrInstance
     * @param params
     * @param args
     */
    public abstract void vException(Object exception, String[] params, Object classOrInstance, Object[] args);


}
