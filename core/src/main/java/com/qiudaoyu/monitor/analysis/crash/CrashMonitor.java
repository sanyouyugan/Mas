package com.qiudaoyu.monitor.analysis.crash;

import android.content.Context;

import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.utils.ProcessUtils;

/**
 * 创建时间: 2018/12/11
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class CrashMonitor extends MetricMonitor implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public CrashMonitor(MetricListener metricListener) {
        super(metricListener);
    }

    public void start(Context context) {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void stop(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(mDefaultHandler);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        metricData(MonitorData.TYPE_METRIC_CRASH, new CrashInfo(System.currentTimeMillis(), t, e));

        if (mDefaultHandler != null) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e1) {
            }
            mDefaultHandler.uncaughtException(t, e);
        } else {
            ProcessUtils.killProcessAndExit();
        }

    }
}
