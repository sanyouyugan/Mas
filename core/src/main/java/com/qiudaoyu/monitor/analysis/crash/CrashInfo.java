package com.qiudaoyu.monitor.analysis.crash;

import com.qiudaoyu.monitor.utils.StacktraceUtil;

import java.io.Serializable;
import java.util.List;


/**
 */
public class CrashInfo implements Serializable {
    public static CrashInfo INVALID = new CrashInfo();
    public long timestampMillis;
    public String threadName;
    public String threadState;
    public String threadGroupName;
    public boolean threadIsDaemon;
    public boolean threadIsAlive;
    public boolean threadIsInterrupted;
    public String throwableMessage;
    public List<String> throwableStacktrace;

    public CrashInfo() {
    }

    public CrashInfo(long timestampMillis, Thread thread, Throwable throwable) {
        this.timestampMillis = timestampMillis;
        this.threadName = thread.getName();
        this.threadState = String.valueOf(thread.getState());
        if (thread.getThreadGroup() != null) {
            this.threadGroupName = String.valueOf(thread.getThreadGroup().getName());
        }
        this.threadIsDaemon = thread.isDaemon();
        this.threadIsAlive = thread.isAlive();
        this.threadIsInterrupted = thread.isInterrupted();
        this.throwableMessage = throwable.getLocalizedMessage();
        this.throwableStacktrace = StacktraceUtil.getStack(throwable.getStackTrace());
    }

    @Override
    public String toString() {
        return "CrashInfo{" +
                "timestampMillis=" + timestampMillis +
                ", threadName='" + threadName + '\'' +
                ", threadState='" + threadState + '\'' +
                ", threadGroupName='" + threadGroupName + '\'' +
                ", threadIsDaemon=" + threadIsDaemon +
                ", threadIsAlive=" + threadIsAlive +
                ", threadIsInterrupted=" + threadIsInterrupted +
                ", throwableMessage='" + throwableMessage + '\'' +
                ", throwableStacktrace=" + throwableStacktrace +
                '}';
    }
}
