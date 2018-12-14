package com.qiudaoyu.monitor.log.dynamiclog.context;

import com.qiudaoyu.monitor.log.dynamiclog.pool.ObjectPool;

/**
 * 创建时间: 2018/9/25
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class MonitorContextFactory implements ObjectPool.RecyclableFactory<MonitorContext> {

    @Override
    public MonitorContext createNew() {
        return new MonitorContext();
    }
}
