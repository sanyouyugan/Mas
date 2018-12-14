package com.qiudaoyu.monitor.log;

import android.util.Log;

import org.apache.commons.lang3.exception.ExceptionUtils;


/**
 * 日志记录在文件中
 * 使用xlog解决性能问题
 */
public class MLog {

    public static String customTagPrefix = "";
    private static boolean debugOrRelease;
    private static LogInterface logInterface;

    public static void initialize(boolean level, String prefix, LogInterface logInterface) {
        customTagPrefix = prefix != null ? prefix : "Mas";
        debugOrRelease = level;
        MLog.logInterface = logInterface;
    }

    private static String generateTag(String tag, StackTraceElement caller) {
        StringBuilder sb = new StringBuilder();
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName
                .lastIndexOf(".") + 1);
        sb.append(customTagPrefix + "-" + tag)
                .append("(")
                .append(callerClazzName + ",")
                .append(caller.getMethodName() + ",")
                .append(Integer.valueOf(caller.getLineNumber()))
                .append(")");
        return sb.toString();
    }

    public static void d(String custag, String content) {
        if (!debugOrRelease) {
            return;
        }
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        if (logInterface != null) {
            logInterface.d(tag, content);
            return;
        }
        Log.d(tag, content);
    }

    public static void d(String custag, String content, Throwable tr) {
        if (!debugOrRelease) {
            return;
        }
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        String contentWithE = "" + content;
        if (tr != null) {
            contentWithE += "\n " + getStackTrace(tr);
        }
        if (logInterface != null) {
            logInterface.d(tag, contentWithE);
            return;
        }
        Log.d(tag, contentWithE);
    }

    public static void e(String custag, String content) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        if (logInterface != null) {
            logInterface.e(tag, content);
            return;
        }
        Log.e(tag, content);
    }

    public static void e(String custag, String content, Throwable tr) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        String contentWithE = "" + content;
        if (tr != null) {
            contentWithE += "\n " + getStackTrace(tr);
        }
        if (logInterface != null) {
            logInterface.e(tag, contentWithE);
            return;
        }
        Log.e(tag, contentWithE);
    }

    public static void i(String custag, String content) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        if (logInterface != null) {
            logInterface.i(tag, content);
            return;
        }
        Log.i(tag, content);
    }

    public static void i(String custag, String content, Throwable tr) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        String contentWithE = "" + content;
        if (tr != null) {
            contentWithE += "\n " + getStackTrace(tr);
        }
        if (logInterface != null) {
            logInterface.i(tag, contentWithE);
            return;
        }
        Log.i(tag, contentWithE);
    }

    public static void v(String custag, String content) {
        if (!debugOrRelease) {
            return;
        }
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        if (logInterface != null) {
            logInterface.v(tag, content);
            return;
        }
        Log.v(tag, content);
    }

    public static void v(String custag, String content, Throwable tr) {
        if (!debugOrRelease) {
            return;
        }
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        String contentWithE = "" + content;
        if (tr != null) {
            contentWithE += "\n " + getStackTrace(tr);
        }

        if (logInterface != null) {
            logInterface.v(tag, contentWithE);
            return;
        }
        Log.v(tag, contentWithE);
    }

    public static void w(String custag, String content) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        if (logInterface != null) {
            logInterface.w(tag, content);
            return;
        }
        Log.w(tag, content);
    }

    public static void w(String custag, String content, Throwable tr) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(custag, caller);
        String contentWithE = "" + content;
        if (tr != null) {
            contentWithE += "\n " + getStackTrace(tr);
        }
        if (logInterface != null) {
            logInterface.w(tag, contentWithE);
            return;
        }
        Log.w(tag, contentWithE);
    }

    public static StackTraceElement getCallerStackTraceElement() {
        return Thread.currentThread().getStackTrace()[4];
    }

    public static void appenderClose() {
        //  Log.appenderClose();
    }

    private static String getStackTrace(Throwable tr) {
        return ExceptionUtils.getStackTrace(tr);
    }

    public interface LogInterface {
        void d(String custag, String content);

        void d(String custag, String content, Throwable tr);

        void e(String custag, String content);

        void e(String custag, String content, Throwable tr);

        void w(String custag, String content);

        void w(String custag, String content, Throwable tr);

        void v(String custag, String content);

        void v(String custag, String content, Throwable tr);

        void i(String custag, String content);

        void i(String custag, String content, Throwable tr);
    }


}