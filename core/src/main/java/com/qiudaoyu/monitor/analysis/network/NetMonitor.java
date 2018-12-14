package com.qiudaoyu.monitor.analysis.network;

import android.content.Context;

import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.analysis.network.http.okhttp.NetWorkInterceptor;
import com.qiudaoyu.monitor.Monitor;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.analysis.network.http.okhttp.NetWorkInterceptor;

import okhttp3.OkHttpClient;

/**
 * 创建时间: 2018/12/11
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class NetMonitor extends MetricMonitor {

    public NetMonitor(MetricListener metricListener) {
        super(metricListener);
    }

    public static void beforeOkHttpBuild(OkHttpClient.Builder builder) {
        if (builder != null && isInstalled(MonitorData.TYPE_METRIC_HTTP)) {
            builder.addInterceptor(new NetWorkInterceptor(new MetricListener() {
                @Override
                public void metric(int type, Object data) {
                    Monitor.metricData(MonitorData.TYPE_METRIC_HTTP, data);
                }
            }));
        }
    }

    public void start(Context context) {

    }

    public void stop(Context context) {

    }
}
