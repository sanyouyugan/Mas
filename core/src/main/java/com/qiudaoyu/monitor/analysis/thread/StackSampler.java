package com.qiudaoyu.monitor.analysis.thread;


import com.qiudaoyu.monitor.analysis.metric.AbstractSampler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 线程堆栈信息dump
 */
public class StackSampler extends AbstractSampler {

    private static final int DEFAULT_MAX_ENTRY_COUNT = 10;
    private static final LinkedHashMap<Long, StackTraceElement[]> sStackMap = new LinkedHashMap<>();

    private int mMaxEntryCount = DEFAULT_MAX_ENTRY_COUNT;
    private Thread mCurrentThread;

    public StackSampler(Thread thread, long sampleIntervalMillis) {
        this(thread, DEFAULT_MAX_ENTRY_COUNT, sampleIntervalMillis);
    }

    public StackSampler(Thread thread, int maxEntryCount, long sampleIntervalMillis) {
        super(sampleIntervalMillis);
        mCurrentThread = thread;
        mMaxEntryCount = maxEntryCount;
    }

    /**
     * 获取这个时间段内dump的堆栈信息
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public Map<Long, StackTraceElement[]> getThreadStackEntries(long startTime, long endTime) {
        Map<Long, StackTraceElement[]> result = new LinkedHashMap<>();
        synchronized (sStackMap) {
            for (Long entryTime : sStackMap.keySet()) {
                if (startTime < entryTime && entryTime < endTime) {
                    result.put(entryTime, sStackMap.get(entryTime));
                }
            }
        }
        return result;
    }


    public void doSample() {
        synchronized (sStackMap) {
            if (sStackMap.size() == mMaxEntryCount && mMaxEntryCount > 0) {
                sStackMap.remove(sStackMap.keySet().iterator().next());
            }
            sStackMap.put(System.currentTimeMillis(), mCurrentThread.getStackTrace());
        }
    }

    public void clear() {
        if (sStackMap != null) {
            sStackMap.clear();
        }
    }
}