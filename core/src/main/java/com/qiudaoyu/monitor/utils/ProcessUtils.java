package com.qiudaoyu.monitor.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * 创建时间: 2018/12/4
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class ProcessUtils {
    /**
     * 获取当前应用uid
     *
     * @return
     */
    public static int getUid(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            //修改
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void killProcessAndExit() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
