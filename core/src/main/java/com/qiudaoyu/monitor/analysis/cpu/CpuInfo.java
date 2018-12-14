package com.qiudaoyu.monitor.analysis.cpu;

import com.qiudaoyu.monitor.analysis.metric.MetricInfo;
import com.qiudaoyu.monitor.analysis.metric.MetricInfo;
import com.qiudaoyu.monitor.log.MLog;

import java.io.Serializable;
import java.util.Locale;

/**
 * Description:
 * 一个sample时间段内
 * 总的cpu使用率
 * app的cpu使用率
 * 用户进程cpu使用率
 * 系统进程cpu使用率
 * io等待时间占比

 */
public class CpuInfo extends MetricInfo implements Serializable{
    public static final CpuInfo INVALID = new CpuInfo();
    // 总的cpu使用率(user + system+io+其他)
    public double totalUseRatio;
    // app的cpu使用率
    public double appCpuRatio;
    // 用户进程cpu使用率
    public double userCpuRatio;
    // 系统进程cpu使用率
    public double sysCpuRatio;
    // io等待时间占比
    public double ioWaitRatio;
    //采样开始时间
    private long startTime;
    //采样结束时间
    private long endTime;

    public CpuInfo(double totalUseRatio, double appCpuRatio, double userCpuRatio, double sysCpuRatio, double
            ioWaitRatio, long startTime, long endTime) {
        this.totalUseRatio = totalUseRatio;
        this.appCpuRatio = appCpuRatio;
        this.userCpuRatio = userCpuRatio;
        this.sysCpuRatio = sysCpuRatio;
        this.ioWaitRatio = ioWaitRatio;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public CpuInfo() {
    }


    public static CpuInfo accumlate(CpuSnapshot startSnapshot, long startTime, CpuSnapshot endSnapshot, long endTime) {

        float totalTime = (endSnapshot.total - startSnapshot.total) * 1.0f;
        if (totalTime <= 0) {
            MLog.d("cpu", "totalTime must greater than 0");
            return CpuInfo.INVALID;
        }
        long idleTime = endSnapshot.idle - startSnapshot.idle;
        double totalRatio = (totalTime - idleTime) / totalTime;
        double appRatio = (endSnapshot.app - startSnapshot.app) / totalTime;
        double userRatio = (endSnapshot.user - startSnapshot.user) / totalTime;
        double systemRatio = (endSnapshot.system - startSnapshot.system) / totalTime;
        double ioWaitRatio = (endSnapshot.ioWait - startSnapshot.ioWait) / totalTime;
        if (!isValidRatios(totalRatio, appRatio, userRatio, systemRatio, ioWaitRatio)) {
            MLog.d("cpu", "not valid ratio");
            return CpuInfo.INVALID;
        }
        return new CpuInfo(totalRatio, appRatio, userRatio, systemRatio, ioWaitRatio, startTime, endTime);
    }

    private static boolean isValidRatios(Double... ratios) {
        for (double ratio : ratios) {
            if (ratio < 0 || ratio > 1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "app:" +
                String.format(Locale.US, "%.1f", appCpuRatio * 100f) +
                "% , total:" +
                String.format(Locale.US, "%.1f", totalUseRatio * 100f) +
                "% , user:" +
                String.format(Locale.US, "%.1f", userCpuRatio * 100f) +
                "% , system:" +
                String.format(Locale.US, "%.1f", sysCpuRatio * 100f) +
                "% , iowait:" +
                String.format(Locale.US, "%.1f", ioWaitRatio * 100f) + "%";
    }
}
