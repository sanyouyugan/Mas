package com.qiudaoyu.monitor.analysis;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.google.gson.Gson;
import com.qiudaoyu.monitor.BuildConfig;
import com.qiudaoyu.monitor.Monitor;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.analysis.activity.MasDataActivityLifecycleCallbacks;
import com.qiudaoyu.monitor.df.MMonitor;
import com.qiudaoyu.monitor.log.MLog;
import com.qiudaoyu.monitor.upload.UploadServiceInterface;
import com.qiudaoyu.monitor.utils.AppUtils;
import com.qiudaoyu.monitor.utils.DeviceUtils;
import com.qiudaoyu.monitor.utils.EmptyUtils;
import com.qiudaoyu.monitor.utils.JSONUtils;
import com.qiudaoyu.monitor.utils.LocationUtils;
import com.qiudaoyu.monitor.utils.MD5Utils;
import com.qiudaoyu.monitor.utils.NetWorkUtils;
import com.tencent.mmkv.MMKV;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static com.tencent.mmkv.MMKV.MULTI_PROCESS_MODE;

/**
 * 业务通过原始数据产生自己需要的数据
 * <p>
 * 重写MasData
 */
public class MasData {

    public static final String EVENT_APPSTART = "$AppStart";
    public static final String EVENT_APPEND = "$AppEnd";
    public static final String EVENT_APPVIEWSCREEN = "$AppViewScreen";
    public static final String EVENT_APPCRASH = "$AppCrashed";
    public static final String APP_CLICK_EVENT_NAME = "$AppClick";

    public static final String ELEMENT_ID = "$element_id";
    public static final String ELEMENT_TYPE = "$element_type";
    public static final String ELEMENT_CONTENT = "$element_content";
    public static final String ELEMENT_POSITION = "$element_position";
    public static final String SCREEN_NAME = "$screen_name";
    public static final String TITLE = "$title";

    private static final String DEVICE_ID = "DEVICE_ID";

    private static final String TAG = "MasData";
    private static ThreadLocal<String> parentId;
    private NetWorkStateReceiver netWorkStateReceiver;
    private Context mContext;
    private ConcurrentHashMap<String, Object> deviceInfo;
    private ConcurrentHashMap<String, Object> dynamicCommon;
    private MMKV kv;
    private volatile AtomicLong openId;
    private volatile boolean isAlive;
    LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            if (isAlive && deviceInfo != null && location != null) {
                LocationUtils.getInstace().setLastLocation(location);
                deviceInfo.put("latitude", location.getLatitude());
                deviceInfo.put("longitude", location.getLongitude());
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String arg0) {


        }

        @Override
        public void onProviderDisabled(String arg0) {

        }

    };
    private volatile Map<String, String> dynamicP;
    private LinkedBlockingQueue<JSONObject> events;
    private Thread workThread;
    private MasDataActivityLifecycleCallbacks callbacks;
    private UploadServiceInterface uploadService;

    private MasData() {
        parentId = new ThreadLocal<>();
        final Context context = Monitor.getContext();
        if (context == null) {
            throw new IllegalArgumentException("initial context can't be null");
        }
        mContext = context;
        kv = MMKV.mmkvWithID(AppUtils.getPkgName(context), MULTI_PROCESS_MODE);

        //进程启动的时候生成新的Id
        openId = new AtomicLong(System.currentTimeMillis());

        //设备数据
        deviceInfo = new ConcurrentHashMap<>();

        //业务变化的数据
        dynamicCommon = new ConcurrentHashMap<>();

        //设备Id
        if (!kv.contains(DEVICE_ID)) {
            kv.putString(DEVICE_ID, DeviceUtils.getDeviceId(context));
        }

        deviceInfo.put("device_id", kv.getString(DEVICE_ID, ""));


        //应用信息
        deviceInfo.put("$lib", "android");
        deviceInfo.put("$lib_version", BuildConfig.SDK_VER);
        try {
            final PackageManager manager = mContext.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            deviceInfo.put("$app_version", info.versionName);
        } catch (final Exception e) {
            MLog.i(TAG, "Exception getting app version name", e);
        }


        //rom信息
        deviceInfo
                .put("$manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER.trim());

        if (TextUtils.isEmpty(Build.MODEL)) {
            deviceInfo.put("$model", "UNKNOWN");
        } else {
            deviceInfo.put("$model", Build.MODEL.trim());
        }
        deviceInfo.put("$os", "android");
        deviceInfo.put("$os_version",
                Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);

        //屏幕信息
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        deviceInfo.put("$screen_height", displayMetrics.heightPixels);
        deviceInfo.put("$screen_width", displayMetrics.widthPixels);


        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            deviceInfo.put("$screen_orientation", "land");
        } else {
            deviceInfo.put("$screen_orientation", "portrait");
        }


        //网络信息
        String carrier = NetWorkUtils.getCarrier(mContext);
        if (!EmptyUtils.isEmpty(carrier)) {
            deviceInfo.put("$carrier", carrier);
        }
        deviceInfo.put("ip", NetWorkUtils.getLocalIp());
        deviceInfo.put("network_type", NetWorkUtils.networkType(mContext));


        //acitvity生命周期的监控
        callbacks = new MasDataActivityLifecycleCallbacks(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = (Application) context.getApplicationContext();
            app.registerActivityLifecycleCallbacks(callbacks);
        }

        //网络信息监听
        netWorkStateReceiver = new NetWorkStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        mContext.registerReceiver(netWorkStateReceiver, filter);

        isAlive = true;
        events = new LinkedBlockingQueue<>();
        workThread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<JSONObject> list = new ArrayList<>();
                mayHavePermisiion();
                while (!Thread.interrupted() && isAlive) {
                    try {
                        list.clear();
                        events.drainTo(list);
                        for (JSONObject pro : list) {
                            trackEvent(pro);
                        }
                        //sleep一段时间，防止过多的被notify
                        try {
                            Thread.sleep(2000);
                        } catch (Exception e) {
                        }
                        mayHavePermisiion();
                    } catch (Exception e) {
                        MLog.e(TAG, "workthread", e);
                    }
                }
            }
        });
        workThread.start();
    }

    public static MasData getInstance() {
        return Single.sInstace;
    }

    /**
     * 获取deviceId
     *
     * @param context
     * @return
     */
    public static String getDeviceId(Context context) {
        if (!MMKV.mmkvWithID(AppUtils.getPkgName(context), MULTI_PROCESS_MODE).contains(DEVICE_ID)) {
            MMKV.mmkvWithID(AppUtils.getPkgName(context), MULTI_PROCESS_MODE).putString(DEVICE_ID, DeviceUtils.getDeviceId(context));
        }
        return MMKV.mmkvWithID(AppUtils.getPkgName(context), MULTI_PROCESS_MODE).getString(DEVICE_ID, "");
    }

    /**
     * 如果没有权限什么
     * ，在有事件需要发送的时候再重新获取一下数据
     */
    private void mayHavePermisiion() {
        if (!isAlive) {
            return;
        }
        if (!LocationUtils.getInstace().isWorking()) {
            LocationUtils.getInstace().initLocation(mContext, locationListener);
        }
        if (LocationUtils.getInstace().getLastLocation() != null && isAlive && deviceInfo != null) {
            deviceInfo.put("latitude", LocationUtils.getInstace().getLastLocation().getLatitude());
            deviceInfo.put("longitude", LocationUtils.getInstace().getLastLocation().getLongitude());
        }

    }

    /**
     * 网络变化重新计算网络
     */
    private void netChanged() {
        String carrier = NetWorkUtils.getCarrier(mContext);
        if (!EmptyUtils.isEmpty(carrier)) {
            deviceInfo.put("$carrier", carrier);
        }
    }

    /**
     * 屏幕方向变化
     */
    public void orientionChanged() {
        if (mContext != null) {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                deviceInfo.put("screen_orientation", "land");
            } else {
                deviceInfo.put("screen_orientation", "portrait");
            }
        }
    }

    /**
     * 在当前线程产生trackId
     * 业务相关的数据
     *
     * @param eventName
     * @param properties
     */
    public void trackBusUserEvent(final String eventName, final JSONObject properties) {
        String track_id = generatTrackId();

        try {
            if (EmptyUtils.isNotEmpty(parentId.get()) && properties != null) {
                properties.put("parent_id", parentId.get());
            }
            parentId.set(track_id);
            properties.put("track_id", track_id);
        } catch (JSONException e) {

        }
        trackEvent(eventName, properties);

    }

    /**
     * 在当前线程产生trackId
     * 业务相关的数据
     *
     * @param eventName
     * @param properties
     */
    public void trackBusUserEvent(final String eventName, final JSONObject properties, String cusParentId) {
        String track_id = generatTrackId();

        try {
            if (EmptyUtils.isNotEmpty(cusParentId) && properties != null) {
                properties.put("parent_id", cusParentId);
            }
            parentId.set(track_id);
            properties.put("track_id", track_id);

        } catch (JSONException e) {
        }
        trackEvent(eventName, properties);
    }

    /**
     * 传递entityId和userId
     *
     * @param entityId
     * @param userId
     */
    public void setUserId(final String entityId, final String userId) {
        dynamicCommon.put("entity_id", entityId);
        dynamicCommon.put("user_id", userId);
    }

    /**
     * shu
     * appkey
     *
     * @param ak
     */
    public void setAppKey(String ak) {
        dynamicCommon.put("app_key", ak);
    }

    /**
     * 将数据加入发送队列里
     *
     * @param eventName
     * @param properties
     */
    public void trackEvent(final String eventName, JSONObject properties) {
        if (EmptyUtils.isEmpty(eventName)) {
            throw new IllegalArgumentException("eventName can not be null !!!");
        }
        if (properties == null) {
            properties = new JSONObject();
        }
        try {
            properties.put("event_type", eventName);
            if (!properties.has("start_timestamp")) {
                properties.put("start_timestamp", System.currentTimeMillis());
            }
            events.offer(properties);
        } catch (JSONException e) {
            MLog.e(TAG, "send event fail1 :\n" + JSONUtils.formatJson(properties.toString()), e);
        }
    }

    /**
     * 头部数据
     * track_id	字符串	每一个操作都具备的唯一id
     * start_timestamp	数值	开始时间，毫秒
     * app_key	字符串
     * <p>
     * event_type	字符串	事件类型，如 打开APP、页面浏览、事件、APP退出、错误日志、主机信息（网速、cpu等）
     * device_id	字符串	设备id，android:IMEI+MAC（平板端获取AndroidID）   IOS:如果开启了IDFA，则获取IDFA，没有开启，就直接获取IDFV
     * <p>
     * user_id	字符串	用户id，游客统一为guest？
     * entity_id	字符串	店铺id
     * <p>
     * open_id	数值	每次app启动生成的id
     * <p>
     * ip	字符串	ip地址
     * latitude	字符串	GPS的纬度，不能获取则不传
     * longitude	字符串	GPS的经度，不能获取则不传
     * network_type	字符串	2g/3g/4g/5g/wifi
     * screen_orientation	字符串	屏幕方向
     * <p>
     * properties	事件数据对象
     *
     * @param
     * @param
     */
    private void trackEvent(JSONObject prWithEvent) {
        try {

            //prWithEvent加入dp内容
            if (dynamicP != null) {
                if (prWithEvent == null) {
                    prWithEvent = new JSONObject();
                }
                for (String key : new ArrayList<>(dynamicP.keySet())) {
                    prWithEvent.put(key, dynamicP.get(key));
                }
            }

            JSONObject sendProperties = new JSONObject();
            //track_id
            if (prWithEvent.has("track_id")) {
                sendProperties.put("track_id", prWithEvent.get("track_id"));
                prWithEvent.remove("track_id");
            } else {
                sendProperties.put("track_id", generatTrackId());
            }


            //start_timestamp
            sendProperties.put("event_type", prWithEvent.get("event_type"));
            prWithEvent.remove("event_type");
            //start_timestamp
            sendProperties.put("start_timestamp", prWithEvent.get("start_timestamp"));
            prWithEvent.remove("start_timestamp");
            //app_key
            sendProperties.put("app_key", dynamicCommon.get("app_key"));

            //device_id
            sendProperties.put("device_id", deviceInfo.get("device_id"));


            //user_id
            if (EmptyUtils.isNotEmpty(dynamicCommon.get("user_id"))) {
                sendProperties.put("user_id", dynamicCommon.get("user_id"));
            }

            //entity_id
            if (EmptyUtils.isNotEmpty(dynamicCommon.get("entity_id"))) {
                sendProperties.put("entity_id", dynamicCommon.get("entity_id"));
            }
            //open_id
            sendProperties.put("open_id", getOpenId());

            //ip
            sendProperties.put("ip", deviceInfo.get("ip"));

            // GPS
            if (EmptyUtils.isNotEmpty(deviceInfo.get("latitude"))) {
                sendProperties.put("latitude", deviceInfo.get("latitude"));
            }
            if (EmptyUtils.isNotEmpty(deviceInfo.get("longitude"))) {
                sendProperties.put("longitude", deviceInfo.get("longitude"));
            }

            //network_type
            sendProperties.put("network_type", deviceInfo.get("network_type"));

            // 屏幕方向
            sendProperties.put("screen_orientation", deviceInfo.get("screen_orientation"));

            //数据部分处理
            sendProperties.put("properties", prWithEvent);

            if (Monitor.isDebug) {
                MLog.d(TAG, "track event:\n" + JSONUtils.formatJson(sendProperties.toString()));
            }
            sendData(sendProperties);
        } catch (Exception e) {
            MLog.e(TAG, "send event fail2 :\n" + JSONUtils.formatJson(prWithEvent.toString()), e);
        }

    }

    /**
     * @param sendProperties
     */
    private void sendData(JSONObject sendProperties) {
        if (uploadService != null) {
            uploadService.sendData(sendProperties);
        }
    }

    /**
     * @return
     */
    private long getOpenId() {
        return openId.get();
    }

    /**
     * 生成新的openId
     *
     * @return
     */
    public void resetOpenId() {
        openId.compareAndSet(openId.get(), System.currentTimeMillis());
    }

    /**
     * 产生 trakcId
     *
     * @return
     */
    public String generatTrackId() {
        StringBuilder builder = new StringBuilder();
        builder.append(MMonitor.getInstance().getAppKey());
        builder.append(kv.getString(DEVICE_ID, ""));
        builder.append(System.currentTimeMillis());
        builder.append(new Random().nextInt());
        return MD5Utils.encode(builder.toString());
    }

    public Map<String, Object> getDeviceInfo() {
        return deviceInfo;
    }

    public void shutDown() {
        isAlive = false;
        if (workThread != null) {
            workThread.interrupt();
            workThread = null;
        }
        if (events != null) {
            events.clear();
            events = null;
        }
        if (kv != null) {
            kv.close();
            kv = null;
        }
        if (mContext != null) {
            LocationUtils.getInstace().shutDown(mContext);
            if (netWorkStateReceiver != null) {
                mContext.unregisterReceiver(netWorkStateReceiver);
            }
            if (callbacks != null) {
                final Application app = (Application) mContext.getApplicationContext();
                app.unregisterActivityLifecycleCallbacks(callbacks);
            }

        }

    }

    /**
     * @param properties
     */
    public void setDynamicProperties(Map<String, String> properties) {
        dynamicP = properties;
    }

    /**
     * 产生metric事件
     * <p>
     * 业务方自己处理
     * <p>
     * Metric
     *
     * @param mType
     * @param info
     */
    public void metricInfo(int mType, Object info) {
        if (info != null) {
            JSONObject object = new JSONObject();
            try {
                object.put("type", MonitorData.getTpye(mType));
                object.put("data", new Gson().toJson(info));
                trackEvent("$" + MonitorData.getTpye(mType), object);
            } catch (JSONException e) {
                MLog.d("metricInfo", "fail", e);
            }
        }
        MLog.d("metricInfo", "mType " + MonitorData.getTpye(mType) + " \n" + new Gson().toJson(info));
    }

    public void setUploadService(UploadServiceInterface uploadService) {
        this.uploadService = uploadService;
    }

    public String getParentId() {
        if (parentId != null) {
            return parentId.get();
        }
        return null;
    }

    /**
     * 设置当前线程后面时序的parentId
     *
     * @param cusParentId
     */
    public void setParentId(String cusParentId) {
        if (parentId != null) {
            parentId.set(cusParentId);
        }

    }

    static class Single {
        static MasData sInstace;

        static {
            sInstace = new MasData();
        }
    }

    class NetWorkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            netChanged();
        }
    }


}


