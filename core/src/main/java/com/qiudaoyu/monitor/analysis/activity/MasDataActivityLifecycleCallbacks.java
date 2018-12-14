package com.qiudaoyu.monitor.analysis.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;

import com.tencent.mmkv.MMKV;
import com.qiudaoyu.monitor.Monitor;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.analysis.MasData;
import com.qiudaoyu.monitor.log.MLog;
import com.qiudaoyu.monitor.utils.AppUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.qiudaoyu.monitor.analysis.MasData.EVENT_APPEND;
import static com.qiudaoyu.monitor.analysis.MasData.EVENT_APPSTART;
import static com.qiudaoyu.monitor.analysis.MasData.EVENT_APPVIEWSCREEN;
import static com.qiudaoyu.monitor.analysis.utils.MasUtils.mergeJSONObject;


/**
 * 暂不考虑多进程条件下 app启动的问题
 * 通过activtiy的生命周期回调计算 app启动，结束，浏览 事件
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MasDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "MasLifecycleCallbacks";

    private static final int DIED_INTERVAL = 30000;

    private static final String FIRST_START = "FIRST_START";

    private static final String FIRST_START_TODDAY = "FIRST_START_TODDAY";

    private static final String HAS_STARTED = "HAS_STARTED";

    private static final String IS_FOR_GROUND = "IS_FOR_GROUND";

    private static final String END_BACK_TIMEOUT = "END_BACK_TIMEOUT";

    private static final String PASUE_TIME = "PASUE_TIME";
    private static final String ACTIVITY_RESUME_TIME = "ACTIVITY_RESUME_TIME";
    private static final String START_TIME = "START_TIME";
    //当天时间
    private static final SimpleDateFormat mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    //倒计时器的，进入后台30s，认为app关闭了
    private static CountDownTimer mCountDownTimer;
    //是否已经启动
    private static boolean hasStart;


    private String lastUrl;

    private boolean resumeFromBackground = false;
    private Activity currentActivity;
    private MMKV kv;
    private boolean isFirst;
    private boolean isFirstToday;


    public MasDataActivityLifecycleCallbacks(Context context) {
        kv = MMKV.mmkvWithID(AppUtils.getCurrentProcessName(context));
        initCountDownTimer();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        Monitor.setForground(true);
        MasData.getInstance().orientionChanged();
    }

    /**
     * activity启动认为是
     * <p>
     * "$resume_from_background":"false",       //类型：BOOL,App 是否是从后台恢复
     * "$is_first_time":"false",                //类型：BOOL,是否首次启动
     * "$is_first_day":"true",                  //类型：BOOL,是否首日访问
     * "$screen_name":"Mainactivy",             //类型：页面名称,Activity 的标题（仅 Android 端有）
     * "$title":"com.zmsoft.Mainactivy.Gradle", //类型：页面标题,Activity 的包名.类名（仅 Android 端有）
     *
     * @param activity
     */
    @Override
    public void onActivityStarted(Activity activity) {

        currentActivity = activity;
        if (!hasStart) {

            hasStart = true;

            synchronized (this) {
                //进程重新启动了
                //判断是否是第一次启动，
                if (!kv.contains(FIRST_START)) {
                    kv.encode(FIRST_START, true);
                    isFirst = true;
                } else {
                    kv.encode(FIRST_START, false);
                    isFirst = false;
                }
                //今天的日期
                String today = mIsFirstDayDateFormat.format(new Date(System.currentTimeMillis()));
                // 是否是今天第一次启动
                if (!kv.contains(FIRST_START_TODDAY)) {
                    isFirstToday = true;
                    kv.encode(FIRST_START_TODDAY, today);
                } else if (!kv.getString(FIRST_START_TODDAY, "").equals(today)) {
                    kv.encode(FIRST_START_TODDAY, today);
                    isFirstToday = true;
                }


                //onPause代表进入后台，如果是被杀死再启动，如果是30s时间到了
                long pasueTime = kv.getLong(PASUE_TIME, 0);
                //认为他pasue的时候是他被杀死的时间,如果第一个启动页面是启动页，说明是正常退出的
                if (pasueTime != 0 && (System.currentTimeMillis() - pasueTime) > DIED_INTERVAL) {
                    trackAppEnd(true, activity);
                }

                //记录进程第一次启动的时间为startTime
                kv.encode(START_TIME, System.currentTimeMillis());
                //重新启动了，生成AppStart事件
                trackAppStart(resumeFromBackground, isFirst, isFirstToday, activity);

            }
        } else {
            if (kv.getBoolean(END_BACK_TIMEOUT, false)) {
                resumeFromBackground = true;
                //从后台重新回到前台，生成AppStart事件
                isFirstToday = false;
                isFirst = false;
                //新的启动时间
                kv.encode(START_TIME, System.currentTimeMillis());
                //重新启动了，生成AppStart事件
                trackAppStart(resumeFromBackground, isFirst, isFirstToday, activity);
                MasData.getInstance().resetOpenId();
            }
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
        }


        kv.encode(HAS_STARTED, true);
        kv.encode(IS_FOR_GROUND, true);
        kv.encode(END_BACK_TIMEOUT, false);
        kv.encode(ACTIVITY_RESUME_TIME, System.currentTimeMillis());
        kv.encode(PASUE_TIME, System.currentTimeMillis());
    }

    /**
     * 启动事件
     * "$lib":"android",              //类型：字符串   SDK类型
     * "$lib_version":"1.1.0",        //类型：字符串   SDK版本
     * "$app_version":"3.5.1",        //类型：字符串   应用版本
     * "$carrier":"ChinaNet",           //类型：字符串，运营商名称
     * "$manufacturer":"vivo",      //类型：字符串，设备制造商，JS获取不到可以不用设置
     * "$model":"vivo X710L",       //类型：字符串，设备型号
     * "$os":"android",              //类型：字符串， 操作系统
     * "$os_version":"android-8.0.1",    //类型：字符串， 操作系统版本
     * "$screen_height":"1920",         //类型：数值，屏幕高度
     * "$screen_width":"1080",          //类型：数值，屏幕宽度
     * "$browser":"Chrome",             //类型：字符串,只有JS时才会设置
     * "$browser_version":"Chrome45",   //类型：字符串,只有JS时才会设置
     * <p>
     * "$resume_from_background":"false",       //类型：BOOL,App 是否是从后台恢复
     * "$is_first_time":"false",                //类型：BOOL,是否首次启动
     * "$is_first_day":"true",                  //类型：BOOL,是否首日访问
     * "$screen_name":"Mainactivy",             //类型：页面名称,Activity 的标题（仅 Android 端有）
     * "$title":"com.zmsoft.Mainactivy.Gradle", //类型：页面标题,Activity 的包名.类名（仅 Android 端有）
     */
    private void trackAppStart(boolean resume_from_background, boolean is_first_time, boolean is_first_day, Activity activity) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_START)) {
            return;
        }

        JSONObject startJson = new JSONObject();
        try {
            startJson.put("start_timestamp", System.currentTimeMillis());
            startJson.put("$title", AppUtils.getActivityTitle(activity));
            startJson.put("$screen_name", activity.getClass().getCanonicalName());
            startJson.put("$is_first_day", is_first_day);
            startJson.put("$is_first_time", is_first_time);
            startJson.put("$resume_from_background", resume_from_background);
            startJson.put("$lib", MasData.getInstance().getDeviceInfo().get("$lib"));
            startJson.put("$lib_version", MasData.getInstance().getDeviceInfo().get("$lib_version"));
            startJson.put("$carrier", MasData.getInstance().getDeviceInfo().get("$carrier"));
            startJson.put("$manufacturer", MasData.getInstance().getDeviceInfo().get("$manufacturer"));
            startJson.put("$model", MasData.getInstance().getDeviceInfo().get("$model"));
            startJson.put("$os", MasData.getInstance().getDeviceInfo().get("$os"));
            startJson.put("$os_version", MasData.getInstance().getDeviceInfo().get("$os_version"));
            startJson.put("$screen_height", MasData.getInstance().getDeviceInfo().get("$screen_height"));
            startJson.put("$screen_width", MasData.getInstance().getDeviceInfo().get("$screen_width"));
            MasData.getInstance().trackEvent(EVENT_APPSTART, startJson);
        } catch (JSONException e) {
            MLog.e("trackAppStart", "", e);
        }
    }

    /**
     * app结束时间
     * "$event_duration":"20",       //类型：数值,本次App启动的使用时长，毫秒
     * "$is_active":"false",   //类型，布尔，是否主动关闭
     * "$screen_name":"Mainactivy",             //类型：页面名称,Activity 的标题（仅 Android 端有）
     * "$title":"com.zmsoft.Mainactivy.Gradle", //类型：页面标题,Activity 的包名.类名（仅 Android 端有）
     */
    private void trackAppEnd(boolean startOrBack, Activity activity) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_END)) {
            return;
        }
        JSONObject startJson = new JSONObject();
        long d = kv.getLong(PASUE_TIME, 0) - kv.getLong(START_TIME, 0);
        try {
            if (startOrBack) {
                startJson.put("start_timestamp", kv.getLong(PASUE_TIME, 0));
                startJson.put("$is_active", AppUtils.isLauncherActivity(activity));
                startJson.put("$title", kv.getString("end_title", ""));
                startJson.put("$screen_name", kv.getString("end_name", ""));
            } else {
                startJson.put("start_timestamp", System.currentTimeMillis());
                startJson.put("$is_active", false);
                startJson.put("$title", AppUtils.getActivityTitle(activity));
                startJson.put("$screen_name", activity.getClass().getCanonicalName());
            }
            startJson.put("$event_duration", d);
            MasData.getInstance().trackEvent(EVENT_APPEND, startJson);
        } catch (JSONException e) {
            MLog.e("trackAppStart", "", e);
        }

    }

    /**
     * "$event_duration":"20",       //类型：数值,浏览时长，毫秒
     * "$screen_name":"Mainactivy",             //类型：页面名称,Activity 的标题（仅 Android 端有）
     * "$title":"com.zmsoft.Mainactivy.Gradle", //类型：页面标题,Activity 的包名.类名（仅 Android 端有）
     * "$url":"tracedata://page/order/detail?orderId=888888",                          //类型：字符串   当前页面URL地址
     * "$referrer":"tracedata://page/order/detail?orderId=888886",            //类型：字符串   前面页面URL地址
     */
    private void trackAppViewScreen(long startTime, long resumtTime, long pasueTime, Activity activity) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_VIEW)) {
            return;
        }

        JSONObject startJson = new JSONObject();
        try {

            //activity是否提供额外的参数
            String url = activity.getClass().getCanonicalName();
            if (activity instanceof MasScreenAutoTracker) {
                MasScreenAutoTracker screenAutoTracker = (MasScreenAutoTracker) activity;
                url = screenAutoTracker.getScreenUrl();
                if (TextUtils.isEmpty(url)) {
                    url = activity.getClass().getCanonicalName();
                }
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    mergeJSONObject(otherProperties, startJson);
                }
            } else {
                MasDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = activity.getClass().getAnnotation(MasDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    String screenUrl = autoTrackAppViewScreenUrl.url();
                    if (TextUtils.isEmpty(screenUrl)) {
                        url = activity.getClass().getCanonicalName();
                    }

                }
            }
            if (!TextUtils.isEmpty(lastUrl)) {
                startJson.put("$referrer", lastUrl);
            }
            lastUrl = url;
            startJson.put("start_timestamp", System.currentTimeMillis());
            startJson.put("$url", url);
            startJson.put("$event_duration", (pasueTime - startTime));
            startJson.put("open_time", (resumtTime - startTime));
            startJson.put("activity_instance", activity.hashCode());
            startJson.put("$title", kv.getString("end_title", ""));
            startJson.put("$screen_name", kv.getString("end_name", ""));
            MasData.getInstance().trackEvent(EVENT_APPVIEWSCREEN, startJson);
        } catch (JSONException e) {
            MLog.e("trackAppStart", "", e);
        }


    }

    @Override
    public void onActivityResumed(final Activity activity) {
        try {
            kv.putLong(ACTIVITY_RESUME_TIME, System.currentTimeMillis());

            //开始就是结束
            kv.putString("end_title", AppUtils.getActivityTitle(activity));
            kv.putString("end_name", activity.getClass().getCanonicalName());
            //每秒计算一次结束事件
            MasDataTimer.getInstance().timer(new Runnable() {
                @Override
                public void run() {
                    kv.putLong(PASUE_TIME, System.currentTimeMillis());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {

        synchronized (this) {
            // cancel TimerTask of current Activity
            MasDataTimer.getInstance().cancleTimerTask();
            //Activity进入pause
            mCountDownTimer.start();
            //计算ViewScreen事件
            kv.putLong(PASUE_TIME, System.currentTimeMillis());
            trackAppViewScreen(kv.getLong(START_TIME, 0), kv.getLong(ACTIVITY_RESUME_TIME, 0), kv.getLong(PASUE_TIME, 0), activity);
        }
    }


    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }


    /**
     * 计算app到后台多长时间
     */
    private void initCountDownTimer() {
        mCountDownTimer = new CountDownTimer(DIED_INTERVAL, 10 * 1000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                MLog.d(TAG, "timeFinish");
                synchronized (MasDataActivityLifecycleCallbacks.this) {
                    mCountDownTimer.cancel();
                    //如果30s还没有AppStart
                    trackAppEnd(false, currentActivity);
                    MasData.getInstance().resetOpenId();
                    Monitor.setForground(false);
                }
            }
        };
    }

}
