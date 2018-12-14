package com.qiudaoyu.monitor.analysis.memory.gc;

import com.qiudaoyu.monitor.analysis.metric.MetricInfo;
import com.qiudaoyu.monitor.analysis.metric.MetricInfo;

/**
 * 创建时间: 2018/12/4
 * 类描述:
 * <p>
 * 收集logcat中的GC日志
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class GcInfo extends MetricInfo {
    //logcat捕捉GC日志
    //Dalvik/Art
    //adb shell logcat com.zmsoft.kds | grep -e GC_ -e AllocSpace

    boolean isArt;
    String gcString;

    public GcInfo(boolean isArt, String gcString) {
        this.isArt = isArt;
        this.gcString = gcString;
    }

}
