package com.qiudaoyu.monitor.analysis.ui.anr;

import com.qiudaoyu.monitor.analysis.metric.MetricInfo;
import com.qiudaoyu.monitor.analysis.metric.MetricInfo;

/**
 * ANR信息类型
 *
 * @author ArgusAPM Team
 */
public class AnrInfo extends MetricInfo {

    private String proName;
    private long time;
    private String anrContent;
    private long proId;

    public String getProName() {
        return proName;
    }

    public void setProName(String proName) {
        this.proName = proName;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getAnrContent() {
        return anrContent;
    }

    public void setAnrContent(String anrContent) {
        this.anrContent = anrContent;
    }

    public long getProId() {
        return proId;
    }

    public void setProId(long proId) {
        this.proId = proId;
    }


}
