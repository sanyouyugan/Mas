package com.qiudaoyu.monitor.analysis.memory.data;

/**
 *
 */

public class PssInfo {
    public int totalPssKb;
    public int dalvikPssKb;
    public int nativePssKb;
    public int otherPssKb;

    @Override
    public String toString() {
        return "PssInfo{" +
                "totalPss=" + totalPssKb +
                ", dalvikPss=" + dalvikPssKb +
                ", nativePss=" + nativePssKb +
                ", otherPss=" + otherPssKb +
                '}';
    }
}
