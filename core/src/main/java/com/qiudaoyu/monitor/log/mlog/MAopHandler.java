package com.qiudaoyu.monitor.log.mlog;

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
public interface MAopHandler {
    /**
     * 如果before有数据需要传递给after,使用ThreadLocal来实现
     *
     * @param classOrInstance
     * @param params
     * @param args
     */
    void before(String[] params, Object classOrInstance, Object[] args);


    /**
     * mode 1-正常返回 0-exception处退出
     *
     * @param mode
     * @param returnValue
     * @param params
     * @param classOrInstance
     * @param args
     */
    void after(int mode, Object returnValue, String[] params, Object classOrInstance, Object[] args);

    /**
     * returnValue
     *
     * @param classOrInstance
     * @param params
     * @param args
     */
    void exception(Object exception, String[] params, Object classOrInstance, Object[] args);


}
