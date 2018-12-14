package com.qiudaoyu.monitor.analysis.memory;

import android.content.Context;

import com.qiudaoyu.monitor.analysis.memory.data.Memory;
import com.qiudaoyu.monitor.analysis.memory.gc.GcInfo;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.analysis.memory.data.Memory;
import com.qiudaoyu.monitor.analysis.memory.gc.Gc;
import com.qiudaoyu.monitor.analysis.memory.gc.GcInfo;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.log.MLog;
import com.qiudaoyu.monitor.utils.ContentLisenter;
import com.qiudaoyu.monitor.utils.DeviceUtils;
import com.qiudaoyu.monitor.utils.ShellUtils;

/**
 * 创建时间: 2018/12/11
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class MemMonitor extends MetricMonitor {

    private Thread workDamondThread;

    private Thread workGcDamondThread;

    public MemMonitor(MetricListener metricListener) {
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
                            synchronized (workDamondThread) {
                                try {
                                    workDamondThread.wait(1000);
                                } catch (InterruptedException e) {
                                }
                            }

                            //收集mem数据
                            metricData(MonitorData.TYPE_METRIC_MEM, Memory.getAppMemInfo(context));
                        }
                    }
                }
            });
            workDamondThread.start();
        }
        if (isInstalled(MonitorData.TYPE_METRIC_GC)) {
            workGcDamondThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (workGcDamondThread) {
                        while (!Thread.interrupted()) {
                            synchronized (workGcDamondThread) {
                                try {
                                    workGcDamondThread.wait(1000);
                                } catch (InterruptedException e) {
                                }
                            }
                            //收集Gc数据
                            ShellUtils.execCommand(new String[]{"logcat", "-v", "time"}, false, new ContentLisenter() {
                                @Override
                                public void content(String content) {
                                    if (content.contains("GC")
                                            && content.contains("paused")
                                            && content.contains("freed")
                                            && content.contains(String.valueOf(android.os.Process.myPid()))
                                            && !content.contains(MLog.customTagPrefix)
                                            && !content.contains("gcString")
                                            && !content.contains("isArt")) {
                                        metricData(MonitorData.TYPE_METRIC_GC, new GcInfo(DeviceUtils.getIsArtInUse(), content.replace("\\", "")));
                                    }
                                }

                                @Override
                                public void error(Exception e, String s) {
                                    if (e != null) {
                                        MLog.d("GC", "error", e);
                                    } else {
                                        MLog.d("GC", "error " + s);
                                    }
                                }
                            });


                        }
                    }
                }
            });
            workGcDamondThread.start();
        }

    }

    @Override
    public void notifyWork() {
        if (workDamondThread != null) {
            synchronized (workDamondThread) {
                workDamondThread.notify();
            }
        }

        if (workGcDamondThread != null) {
            synchronized (workGcDamondThread) {
                workGcDamondThread.notify();
            }
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

        if (workGcDamondThread != null) {
            synchronized (workGcDamondThread) {
                workGcDamondThread.notify();
            }
            workGcDamondThread.interrupt();
            workGcDamondThread = null;
        }

    }
}
