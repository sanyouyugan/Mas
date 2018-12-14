package com.qiudaoyu.monitor.analysis.activity;

import java.util.Timer;
import java.util.TimerTask;

public class MasDataTimer {

    private Timer mTimer;
    private TimerTask mTimerTask;
    private static MasDataTimer instance;
    private final int TIME_INTERVAL = 1000;

    public static MasDataTimer getInstance() {
        if (instance == null) {
            instance = new MasDataTimer();
        }
        return instance;
    }

    private MasDataTimer() {
    }

    /**
     * start a timer task
     * @param runnable
     */
    public void timer(final Runnable runnable) {
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(mTimerTask, 0, TIME_INTERVAL);
        } else {
            mTimer.schedule(mTimerTask, 0, TIME_INTERVAL);
        }
    }

    /**
     * cancel timer task
     */
    public void cancleTimerTask() {
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }
}
