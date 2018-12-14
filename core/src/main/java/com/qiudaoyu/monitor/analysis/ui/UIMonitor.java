package com.qiudaoyu.monitor.analysis.ui;

import android.content.Context;
import android.os.Looper;

import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.analysis.thread.StackSampler;
import com.qiudaoyu.monitor.analysis.ui.anr.Anr;
import com.qiudaoyu.monitor.analysis.ui.anr.AnrFileParser;
import com.qiudaoyu.monitor.analysis.ui.anr.AnrInfo;
import com.qiudaoyu.monitor.analysis.ui.blockdetector.BlockInfo;
import com.qiudaoyu.monitor.analysis.ui.blockdetector.LooperMonitor;
import com.qiudaoyu.monitor.analysis.ui.fps.Fps;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.analysis.thread.StackSampler;
import com.qiudaoyu.monitor.analysis.ui.anr.Anr;
import com.qiudaoyu.monitor.analysis.ui.anr.AnrFileParser;
import com.qiudaoyu.monitor.analysis.ui.anr.AnrInfo;
import com.qiudaoyu.monitor.analysis.ui.blockdetector.BlockInfo;
import com.qiudaoyu.monitor.analysis.ui.blockdetector.LooperMonitor;
import com.qiudaoyu.monitor.analysis.ui.fps.Fps;

/**
 * 创建时间: 2018/12/2
 * 类描述:
 * <p>
 * printer式：结束时才知道卡顿，主线程堆栈获取的不准确
 * <p>
 * watchdog式的：堆栈获取的准确度高一些
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class UIMonitor extends MetricMonitor {

    public final static long SHORT_BLOCK_THRESHOLD = 200;
    public final static long LONG_BLOCK_THRESHOLD = 1000;

    public final static long ANR_THRESHOLD = 5000;
    private Thread workDamondThread;
    private StackSampler stackSampler;

    private LooperMonitor monitor;

    public UIMonitor(MetricListener metricListener) {
        super(metricListener);
    }

    public void start(final Context context) {
        if (isInstalled(MonitorData.TYPE_METRIC_ANR)
                || isInstalled(MonitorData.TYPE_METRIC_FPS)) {
            if (isInstalled(MonitorData.TYPE_METRIC_ANR)) {
                Anr.start();
            }
            if (isInstalled(MonitorData.TYPE_METRIC_FPS)) {
                Fps.start();
            }
            workDamondThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (workDamondThread) {
                        while (!Thread.interrupted()) {
                            try {
                                workDamondThread.wait(3000);
                            } catch (InterruptedException e) {
                            }

                            //查看是否有Anr事件
                            if (Anr.hasAnr()) {
                                Anr.clearAnr();
                                AnrInfo info = AnrFileParser.parseFile(context, Anr.ANR_DIR);
                                if (info != null) {
                                    Anr.save(info);
                                    //anr数据
                                    metricData(MonitorData.TYPE_METRIC_ANR, info);
                                }
                            }
                            if (isInstalled(MonitorData.TYPE_METRIC_FPS)) {
                                //收集fps数据
                                metricData(MonitorData.TYPE_METRIC_FPS, Fps.getFpsInfos());
                            }
                        }
                    }
                }
            });
            workDamondThread.start();
        }

        if (isInstalled(MonitorData.TYPE_METRIC_BLOCK)) {
            stackSampler = new StackSampler(
                    Looper.getMainLooper().getThread(), LONG_BLOCK_THRESHOLD / (2 * 5));

            monitor = new LooperMonitor(new LooperMonitor.BlockListener() {
                @Override
                public void onEventMayLongBlock(long time) {
                    stackSampler.start();
                }

                @Override
                public void onEventStart(long startTime) {
                }

                @Override
                public void onEventEnd(long endTime) {
                    stackSampler.stop();
                }

                @Override
                public void onBlockEvent(final long blockTimeMillis, final long threadBlockTimeMillis, final boolean longBlock, final long eventStartTimeMilliis, final long eventEndTimeMillis, long longBlockThresholdMillis, long shortBlockThresholdMillis) {
                    //上传卡顿事件
                    metricData(MonitorData.TYPE_METRIC_BLOCK,
                            new BlockInfo(eventStartTimeMilliis, blockTimeMillis, threadBlockTimeMillis, longBlock, stackSampler.getThreadStackEntries(eventStartTimeMilliis, eventEndTimeMillis)));
                    //去掉sample数据
                    stackSampler.clear();

                    if (threadBlockTimeMillis > ANR_THRESHOLD) {
                        notifyWork();
                    }
                }
            }, LONG_BLOCK_THRESHOLD, SHORT_BLOCK_THRESHOLD);
            Looper.getMainLooper().setMessageLogging(monitor);
            monitor.start();
        }
    }


    public void stop(Context context) {
        if (workDamondThread != null) {
            synchronized (workDamondThread) {
                workDamondThread.notify();
            }
            workDamondThread.interrupt();
            workDamondThread = null;
        }
        if (monitor != null) {
            monitor.stop();
        }
        if (stackSampler != null) {
            stackSampler.stop();
        }
        Anr.stop();
        Fps.stop();
        Looper.getMainLooper().setMessageLogging(null);
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
