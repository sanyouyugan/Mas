package com.qiudaoyu.monitor.analysis.ui.fps;

import com.qiudaoyu.monitor.analysis.metric.MetricInfo;
import com.qiudaoyu.monitor.analysis.metric.MetricInfo;

/**
 * fps收集task
 */
public class FpsInfo extends MetricInfo {

    long start;
    long end;
    int count;

    public FpsInfo(long start, long end, int count) {
        this.start = start;
        this.end = end;
        this.count = count;
    }
}