package com.qiudaoyu.monitor.analysis.ui.blockdetector;

import com.qiudaoyu.monitor.analysis.metric.MetricInfo;
import com.qiudaoyu.monitor.analysis.metric.MetricInfo;
import com.qiudaoyu.monitor.utils.StacktraceUtil;

import java.util.Map;

/**
 * 卡顿数据
 */
public class BlockInfo extends MetricInfo {

    /**
     * 卡顿所在线程消耗的时间
     * 如果线程消耗时间和实际时间（costTime）差不多，那么说明在这个线程上（主线程）执行某个任务太耗时
     * 如果线程消耗时间远小于实际时间，那么说明这个线程正在等待资源（等待资源耗时）
     */
    public long threadTimeCost;

    public long start;

    public long costTime;

    public boolean isLongBlock;

    public Map<String, String> stackSamples;

    public BlockInfo(long start, long costTime, long threadTimeCost, boolean isLongBlock, Map<Long, StackTraceElement[]> stackSamples) {
        this.start = start;
        this.costTime = costTime;
        this.threadTimeCost = threadTimeCost;
        this.isLongBlock = isLongBlock;
        this.stackSamples = StacktraceUtil.convertToStackString(stackSamples);

    }
}