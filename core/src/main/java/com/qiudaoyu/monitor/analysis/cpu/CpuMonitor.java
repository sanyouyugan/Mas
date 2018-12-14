package com.qiudaoyu.monitor.analysis.cpu;

import android.content.Context;

import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;

/**
 * 创建时间: 2018/12/11
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class CpuMonitor extends MetricMonitor {

    private Thread workDamondThread;

    private volatile int waitTime = 500;

    public CpuMonitor(MetricListener metricListener) {
        super(metricListener);
    }

    @Override
    public void start(final Context context) {
        if (isInstalled(MonitorData.TYPE_METRIC_MEM)) {
            workDamondThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (workDamondThread) {
                        while (!Thread.interrupted()) {
                            long start = System.currentTimeMillis();
                            CpuSnapshot cpuSnapshot = CpuSnapshot.getCpuUsage();
                            try {
                                workDamondThread.wait(waitTime);
                            } catch (InterruptedException e) {
                            }
                            long end = System.currentTimeMillis();
                            CpuSnapshot cpuSnapshotEnd = CpuSnapshot.getCpuUsage();
                            //收集mem数据
                            metricData(MonitorData.TYPE_METRIC_CPU, CpuInfo.accumlate(cpuSnapshot, start, cpuSnapshotEnd, end));
                        }
                    }
                }
            });
            workDamondThread.start();
        }
    }

    @Override
    public void stop(Context context) {
        if (workDamondThread != null) {
            synchronized (workDamondThread) {
                workDamondThread.notify();
            }
            workDamondThread.interrupt();
            workDamondThread = null;
        }
    }

    @Override
    public void notifyWork() {
        if (workDamondThread != null) {
            synchronized (workDamondThread) {
                workDamondThread.notify();
            }
        }
    }
}
