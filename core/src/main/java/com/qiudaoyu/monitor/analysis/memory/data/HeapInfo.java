package com.qiudaoyu.monitor.analysis.memory.data;

/**
 *
 */
public class HeapInfo {
    public long freeMemKb;
    public long maxMemKb;
    public long allocatedKb;

    @Override
    public String toString() {
        return "HeapInfo{" +
                "freeMemKb=" + freeMemKb +
                ", maxMemKb=" + maxMemKb +
                ", allocatedKb=" + allocatedKb +
                '}';
    }
}
