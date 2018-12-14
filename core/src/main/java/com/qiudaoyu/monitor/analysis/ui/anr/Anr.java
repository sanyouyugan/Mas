package com.qiudaoyu.monitor.analysis.ui.anr;

import android.os.FileObserver;

import com.tencent.mmkv.MMKV;
import com.qiudaoyu.monitor.log.MLog;

/**
 * 创建时间: 2018/12/7
 * 类描述:
 * <p>
 * anr文件格式，匹配出当前进程的内容
 * <p>
 * Android系统API提供了FileObserver抽象类（Linux的INotify机制）来监听系统/sdcard中的文件或文件夹，
 * FileObserver类是一个用于监听文件访问、创建、修改、删除、移动等操作的监听器，基于linux的inotify。
 * FileObserver 是个抽象类，必须继承它才能使用。每个FileObserver对象监听一个单独的文件或者文件夹，如果监视的是一个文件夹，
 * 那么文件夹下所有的文件和级联子目录的改变都会触发监听的事件。
 * 其实不然，经过测试并不支持递归，对于监听目录的子目录中的文件改动，FileObserver 对象是无法收到事件回调的，不仅这样，
 * 监听目录的子目录本身的变动也收不到事件回调。原因是由 linux 的 inotify 机制本身决定的，基于 inotify 实现的 FileObserver 自然也不支持递归监听。
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class Anr {
    public static final String ANR_DIR = "/data/anr/traces.txt";
    public static final long THRESHOLD = 5000;
    public static long lastNotifyTime;

    static FileObserver fileObserver = new FileObserver(ANR_DIR, FileObserver.CREATE | FileObserver.CLOSE_WRITE) {
        @Override
        public void onEvent(int event, String simplePath) {
            MLog.d("anr", "anr happen : event " + event + " | path " + simplePath);
            long time = System.currentTimeMillis();
            if ((time - lastNotifyTime) > THRESHOLD) {
                //读取anr文件，提取到当前进程可能有的Anr信息
                MMKV.mmkvWithID("monitor").putLong(ANR_DIR, time);
            }
            lastNotifyTime = time;
        }
    };


    public static void start() {
        fileObserver.startWatching();
    }

    public static void stop() {
        fileObserver.stopWatching();
    }

    public static boolean hasAnr() {
        return MMKV.mmkvWithID("monitor").getLong(ANR_DIR, 0) != 0;
    }

    public static void clearAnr() {
        MMKV.mmkvWithID("monitor").remove(ANR_DIR);
    }

    public static void save(AnrInfo info) {
        if (info != null) {
            AnrFileParser.setUploadedKey(info.getProId(), info.getTime());
        }
    }

}
