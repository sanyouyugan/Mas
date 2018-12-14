package com.qiudaoyu.monitor.analysis.ui.fps;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.Choreographer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * fps收集task
 */
public class Fps {

    private static Choreographer.FrameCallback frameCallback;
    private static int count;
    private static long start;
    private static long end;

    private static BlockingQueue<FpsInfo> infos;

    public static void start() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            infos = new LinkedBlockingQueue<>();
            frameCallback = new Choreographer.FrameCallback() {//系统绘帧回调
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                public void doFrame(long frameTimeNanos) {
                    if (count == 0) {
                        start = System.currentTimeMillis();
                    }
                    end = System.currentTimeMillis();
                    if (count >= 60 || (end - start) >= 1000) {
                        //整理帧的数据
                        addFpsInfo(new FpsInfo(start, end, count));
                        count = 0;
                    } else {
                        count++;
                    }
                    // 开启下一个doFrame监控
                    Choreographer.getInstance().postFrameCallback(frameCallback);
                }


            };
            Choreographer.getInstance().postFrameCallback(frameCallback);
        }
    }

    public static void stop() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            Choreographer.getInstance().postFrameCallback(null);
        }
        if (infos != null) {
            infos.clear();
            infos = null;
        }
    }

    private static void addFpsInfo(FpsInfo fps) {
        if (infos != null && fps != null) {
            infos.offer(fps);
        }
    }

    public static List<FpsInfo> getFpsInfos() {
        if (infos == null) {
            return null;
        }
        List<FpsInfo> list = new ArrayList<>();
        infos.drainTo(list);
        return list;
    }
}