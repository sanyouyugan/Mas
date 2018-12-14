package com.qiudaoyu.monitor.utils;

/**
 * 创建时间: 2018/9/25
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class ThreadUtils {

    /**
     * 获取当前所有激活的线程
     * @return
     */
    public static Thread[] getAllThreads() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup topGroup = group;
        // 遍历线程组树，获取根线程组
        while (group != null) {
            topGroup = group;
            group = group.getParent();
        }
        // 激活的线程数再加一倍，防止枚举时有可能刚好有动态线程生成
        int slackSize = topGroup.activeCount() * 2;
        Thread[] slackThreads = new Thread[slackSize];
        // 获取根线程组下的所有线程，返回的actualSize便是最终的线程数
        int actualSize = topGroup.enumerate(slackThreads);
        Thread[] atualThreads = new Thread[actualSize];
        // 复制slackThreads中有效的值到atualThreads
        System.arraycopy(slackThreads, 0, atualThreads, 0, actualSize);
        System.out.println("Threads size is " + atualThreads.length);
        return atualThreads;
    }



}
