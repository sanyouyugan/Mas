package com.qiudaoyu.monitor.log.mlog;

/**
 * 创建时间: 2018/10/15
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public abstract class TrackRunable implements Runnable {
    public abstract String getParentId();
}
