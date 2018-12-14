package com.qiudaoyu.monitor.analysis.ui.blockdetector;

import android.os.SystemClock;
import android.util.Printer;

public class LooperMonitor implements Printer {
    public static final String TAG = "LooperMonitor";
    // 长卡顿的阀值
    private long mLongBlockThresholdMillis;
    // 短卡顿的阀值
    private long mShortBlockThresholdMillis;
    // 一次事件开始时间
    private long mThisEventStartTime = 0;
    // 一次事件开始时间（线程内）
    private volatile long mThisEventStartThreadTime = 0;

    private BlockListener mBlockListener;
    // 事件开始标记
    private volatile boolean mEventStart = false;
    private Thread damondThread;
    private volatile long waitTime;
    private volatile int count;

    public LooperMonitor(BlockListener blockListener, long longBlockThresholdMillis, long shortBlockThresholdMillis) {
        if (blockListener == null) {
            throw new IllegalArgumentException("blockListener should not be null.");
        }
        mBlockListener = blockListener;
        mLongBlockThresholdMillis = longBlockThresholdMillis;
        mShortBlockThresholdMillis = shortBlockThresholdMillis;
        waitTime = longBlockThresholdMillis / 2;
        if (waitTime < shortBlockThresholdMillis) {
            waitTime = shortBlockThresholdMillis;
        }
    }

    /**
     * 更新阀值配置
     *
     * @param shortBlockThresholdMillis
     * @param longBlockThresholdMillis
     */
    public void setBlockThreshold(long shortBlockThresholdMillis, long longBlockThresholdMillis) {
        this.mShortBlockThresholdMillis = shortBlockThresholdMillis;
        this.mLongBlockThresholdMillis = longBlockThresholdMillis;
    }

    @Override
    public void println(String x) {
        if (count > 5) {
            synchronized (damondThread) {
                damondThread.notify();
            }
        }
        if (!mEventStart) {// 事件开始
            mThisEventStartTime = System.currentTimeMillis();
            mThisEventStartThreadTime = SystemClock.currentThreadTimeMillis();
            mEventStart = true;
            mBlockListener.onEventStart(mThisEventStartTime);
        } else {// 事件结束
            final long thisEventEndTime = System.currentTimeMillis();
            final long thisEventThreadEndTime = SystemClock.currentThreadTimeMillis();
            mEventStart = false;
            long eventCostTime = thisEventEndTime - mThisEventStartTime;
            long eventCostThreadTime = thisEventThreadEndTime - mThisEventStartThreadTime;
            if (eventCostTime >= mLongBlockThresholdMillis) {
                mBlockListener.onBlockEvent(eventCostTime, eventCostThreadTime, true, mThisEventStartTime,
                        thisEventEndTime, mLongBlockThresholdMillis, mShortBlockThresholdMillis);
            } else if (eventCostTime >= mShortBlockThresholdMillis) {
                mBlockListener.onBlockEvent(eventCostTime, eventCostThreadTime, false, mThisEventStartTime,
                        thisEventEndTime, mLongBlockThresholdMillis, mShortBlockThresholdMillis);
            }
            mBlockListener.onEventEnd(thisEventEndTime);
        }
    }

    String getStackString(StackTraceElement[] traceElements) {
        if (traceElements == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement s : traceElements) {
            sb.append(s.toString() + "\n");
        }
        return sb.toString();
    }

    public void start() {
        damondThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long lastStart = mThisEventStartThreadTime;
                    while (!Thread.interrupted()) {
                        synchronized (damondThread) {
                            if (count <= 5) {
                                damondThread.wait(waitTime);
                            } else {
                                //如果5个waitTime之内，都没新的事件执行，就长等待
                                damondThread.wait();
                                lastStart = mThisEventStartThreadTime;
                            }
                            count = 0;
                        }
                        if (lastStart != mThisEventStartThreadTime || !mEventStart) {
                            count++;
                            //上次事件到这次事件没有超过waitTime,获取thread已经停止处理事件了
                            lastStart = mThisEventStartThreadTime;
                            continue;
                        }
                        //当前事件至少已经执行了waitTime，存在长暂停的风险，开始提前准备堆栈
                        if (mBlockListener != null) {
                            mBlockListener.onEventMayLongBlock(System.currentTimeMillis() - mThisEventStartThreadTime);
                        }

                    }
                } catch (InterruptedException e) {
                }
            }
        });
        damondThread.start();
    }

    public void stop() {
        if (damondThread != null) {
            damondThread.interrupt();
            damondThread = null;
        }
    }

    public interface BlockListener {

        /**
         * 主线程开始处理message
         *
         * @param time
         */
        void onEventMayLongBlock(long time);

        /**
         * 主线程开始处理message
         *
         * @param startTime
         */
        void onEventStart(long startTime);

        /**
         * 主线程结束处理message
         *
         * @param endTime
         */
        void onEventEnd(long endTime);

        /**
         * 发生超过阀值的卡顿事件
         *
         * @param eventStartTimeMilliis     事件开始时间
         * @param eventEndTimeMillis        事件结束时间
         * @param blockTimeMillis           卡顿时间（事件处理时间）
         * @param threadBlockTimeMillis     事件真实消耗时间
         * @param longBlockThresholdMillis  长卡顿阀值标准
         * @param shortBlockThresholdMillis 短卡顿阀值标准
         */
        void onBlockEvent(long blockTimeMillis, long threadBlockTimeMillis, boolean longBlock,
                          long eventStartTimeMilliis, long eventEndTimeMillis, long longBlockThresholdMillis,
                          long shortBlockThresholdMillis);
    }


}