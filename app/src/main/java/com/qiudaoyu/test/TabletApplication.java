package com.qiudaoyu.test;

import android.annotation.SuppressLint;
import android.app.Application;
import android.support.annotation.Keep;
import android.util.Log;

import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.df.MMonitor;
import com.qiudaoyu.monitor.log.annotation.IgnoreLog;
import com.qiudaoyu.monitor.log.mlog.MethodValueAopHandler;

/**
 * 创建时间: 2017/8/14 下午2:12
 * 类描述:
 *
 * @author 木棉
 */
@Keep
@IgnoreLog
public class TabletApplication extends Application {

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();

        // if (AppUtils.isMainProcess(this, BuildConfig.APPLICATION_ID)) {
        MMonitor.Builder.newBuilder().context(this)
                .appInfo("com.qiudaoyu.kds", BuildConfig.VERSION_NAME)
                .appKey("200020", "guce5uq2mbp0t7rn1eg7yrnd7gt0yg4e")
                .uploadService(MMonitor.TEST_URL, MMonitor.TEST_PROT, "/sdcard/kds/upload", (short) 200)
                .isDebugEnalbe(BuildConfig.DEBUG)
                .mAopHandler(new MethodValueAopHandler() {

                    @Override
                    public void vBefore(String[] params, Object classOrInstance, Object[] args) {

                    }

                    @Override
                    public void vAfter(int mode, Object returnValue, String[] params, Object classOrInstance, Object[] args) {
                        Log.e("after", "" + params.toString() + " " + returnValue + " ");
                    }

                    @Override
                    public void vException(Object exception, String[] params, Object classOrInstance, Object[] args) {


                    }
                })
                .MLog(BuildConfig.DEBUG, "KDS", null)
                .install(new int[]{
                        MonitorData.TYPE_METRIC_ANR,
                        MonitorData.TYPE_METRIC_BLOCK,
                        MonitorData.TYPE_METRIC_GC,
                        MonitorData.TYPE_METRIC_HTTP,
                        MonitorData.TYPE_APP_START,
                        MonitorData.TYPE_APP_END,
                        MonitorData.TYPE_APP_CLICK,
                        MonitorData.TYPE_APP_VIEW,
                })
                .build();

        //开始监控
        MMonitor.getInstance().start();


        //登录之后设置userId和entityId
        MMonitor.getInstance().setUserId("000001", "abcdefghigk");
        //}

    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MMonitor.getInstance().onLowMemory();

    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        MMonitor.getInstance().onTrimMemory(level);

    }
}
