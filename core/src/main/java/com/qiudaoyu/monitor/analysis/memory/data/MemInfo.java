package com.qiudaoyu.monitor.analysis.memory.data;

import com.qiudaoyu.monitor.analysis.metric.MetricInfo;
import com.qiudaoyu.monitor.analysis.metric.MetricInfo;

/**
 *
 */
public class MemInfo extends MetricInfo {

    //java堆数据
    public long freeMemKb;
    public long maxMemKb;
    public long allocatedKb;

    //native
    public long nativeAllocatedKb;

    //进程总内存
    public long proMemKb;

    //可用RAM
    public long availMemKb;
    //手机总RAM
    public long totalMemKb;
    //内存占用满的阀值，超过即认为低内存运行状态，可能会Kill process
    public long lowMemThresholdKb;
    //是否低内存状态运行
    public boolean isLowMemory;

    public  long time;

}
