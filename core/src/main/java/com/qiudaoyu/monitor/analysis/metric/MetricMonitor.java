package com.qiudaoyu.monitor.analysis.metric;

import android.content.Context;

import com.qiudaoyu.monitor.Monitor;

/**
 * 创建时间: 2018/12/11
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class MetricMonitor {
    protected MetricListener metricListener;

    protected volatile int sampleInterval;


    public MetricMonitor(MetricListener metricListener) {
        this.metricListener = metricListener;
    }

    public static boolean isInstalled(int type) {
        return Monitor.isInstalled(type);
    }

    protected void metricData(int type, Object data) {
        if (metricListener != null) {
            metricListener.metric(type, data);
        }
    }

    public void start(Context context) {

    }

    public void stop(Context context) {

    }

    public void setForground(boolean isForground) {

    }


    public void notifyWork() {

    }
}
