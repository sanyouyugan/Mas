package com.qiudaoyu.monitor.analysis.activity;


import com.qiudaoyu.monitor.analysis.MasData;
import com.qiudaoyu.monitor.log.MLog;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class MasDataExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final int SLEEP_TIMEOUT_MS = 3000;

    private static MasDataExceptionHandler sInstance;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    private MasDataExceptionHandler() {
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static MasDataExceptionHandler getsInstance() {
        return Single.sInstace;
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        // Only one worker thread - giving priority to storing the event first and then flush

        try {
            final JSONObject messageProp = new JSONObject();
            MasDataTimer.getInstance().cancleTimerTask();
            try {
                Writer writer = new StringWriter();
                PrintWriter printWriter = new PrintWriter(writer);
                e.printStackTrace(printWriter);
                Throwable cause = e.getCause();
                while (cause != null) {
                    cause.printStackTrace(printWriter);
                    cause = cause.getCause();
                }
                printWriter.close();
                String result = writer.toString();
                messageProp.put("$app_crashed_reason", result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            MasData.getInstance().trackEvent(MasData.EVENT_APPCRASH, messageProp);
        } catch (Exception e1) {
            MLog.e("MasE", "", e);
        }


        if (mDefaultExceptionHandler != null) {
            try {
                Thread.sleep(SLEEP_TIMEOUT_MS);
            } catch (InterruptedException e1) {
            }
            mDefaultExceptionHandler.uncaughtException(t, e);
        } else {
            killProcessAndExit();
        }
    }

    private void killProcessAndExit() {
        try {
            Thread.sleep(SLEEP_TIMEOUT_MS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    static class Single {
        static MasDataExceptionHandler sInstace;

        static {
            sInstace = new MasDataExceptionHandler();
        }
    }
}
