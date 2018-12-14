package com.qiudaoyu.monitor.analysis.network.http;

import com.qiudaoyu.monitor.analysis.metric.MetricInfo;
import com.qiudaoyu.monitor.analysis.metric.MetricInfo;

/**
 * 创建时间: 2018/12/5
 * 类描述:
 */
public class HttpInfo extends MetricInfo {

    public String trackId;

    public long requestSize;

    public long responseSize;

    public long startTime;

    public long costTime;
    public Exception reqExcetion;
    public String url;
    /**
     * 请求结果
     */
    public int code;
    public String exceptionString;
    /**
     * 进程时间
     */
    public long fetchStart;
    /**
     * dns
     */
    public long domainLookupStart;

    public long domainLookupEnd;
    /**
     * connect
     */
    public long connectStart;
    /**
     * ssl connect
     */
    public long secureConnectionStart;
    public long secureConnectionEnd;
    public long connectEnd;
    /**
     * http send
     */
    public long requestStart;
    public long requestEnd;
    /**
     * http rec
     */
    public long responseStart;

    public long firstPkg;

    public long responseEnd;


    public void setException(Exception e) {
        reqExcetion = e;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

}
