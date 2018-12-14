package com.qiudaoyu.monitor.analysis.metric;

/**
 * 创建时间: 2018/12/11
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public interface MetricListener {
    void metric(int type, Object data);
}
