package com.qiudaoyu.monitor.df;

import android.content.Context;

import com.qiudaoyu.monitor.Monitor;
import com.qiudaoyu.monitor.analysis.MasData;
import com.qiudaoyu.monitor.df.config.DFConfigManager;
import com.qiudaoyu.monitor.df.upload.MUploadService;
import com.qiudaoyu.monitor.log.MLog;
import com.qiudaoyu.monitor.log.mlog.MethodValueAopHandler;
import com.qiudaoyu.monitor.utils.AppUtils;
import com.qiudaoyu.monitor.utils.EmptyUtils;
import com.qiudaoyu.monitor.BuildConfig;

import org.json.JSONObject;

import java.util.Map;

/**
 *
 *
 */
public class MMonitor {
    public final static String TEST_URL = "10.1.27.50";

    public final static int TEST_PROT = 10000;

    private static MMonitor msInstance;
    private MUploadService.Info info;
    private MasData masData;
    private boolean debug;

    private MMonitor(MUploadService.Info info, boolean isDebug) {
        this.info = info;
        debug = isDebug;
        masData = MasData.getInstance();
        masData.setAppKey(info.appkey);
    }

    public static MMonitor getInstance() {
        if (msInstance == null) {
            throw new IllegalStateException("you should build MMonitor fisrt!!!");
        }
        return msInstance;
    }

    public MUploadService.Info getInfo() {
        return info;
    }

    /**
     * 发送自定义数据
     *
     * @param eventType
     * @param propety
     */
    public void trackBusUserEvent(String eventType, JSONObject propety) {
        if (msInstance == null) {
            throw new IllegalStateException("you should init monitor fisrt!!!");
        }

        if (EmptyUtils.isEmpty(eventType)) {
            throw new IllegalArgumentException("eventType can not be null or empty!!!");
        }
        if (propety == null) {
            propety = new JSONObject();
        }
        masData.trackBusUserEvent(eventType, propety);
    }

    /**
     * 发送自定义数据
     *
     * @param eventType
     * @param propety
     */
    public void trackBusUserEvent(String eventType, JSONObject propety, String parId) {
        if (msInstance == null) {
            throw new IllegalStateException("you should init monitor fisrt!!!");
        }

        if (EmptyUtils.isEmpty(eventType)) {
            throw new IllegalArgumentException("eventType can not be null or empty!!!");
        }
        if (propety == null) {
            propety = new JSONObject();
        }
        masData.trackBusUserEvent(eventType, propety, parId);
    }


    /**
     * 获取当前线程前置操作的Id
     */
    public String getParentId() {
        return masData.getParentId();
    }

    /**
     * @param cusParentId
     */
    public void setParentId(String cusParentId) {
        masData.setParentId(cusParentId);

    }

    /**
     * 传递entityId和userId
     *
     * @param entityId
     * @param userId
     */
    public void setUserId(final String entityId, final String userId) {
        masData.setUserId(entityId, userId);
    }

    /**
     * 设置每个时间都携带的业务数据
     *
     * @param properties
     */
    public void setDynamicProperties(Map<String, String> properties) {
        masData.setDynamicProperties(properties);
    }

    /**
     * 需要权限
     */
    public void start() {
        MUploadService.getInstance().initialize(info);
        if (AppUtils.isMainProcess(info.context, info.pkgName)) {
            Monitor.start();
        }
    }

    public String getAppKey() {
        return info.appkey;
    }

    /**
     *
     */
    public void destroy() {
        if (masData != null) {
            masData.shutDown();
        }
        Monitor.shutDown();
        MUploadService.getInstance().shutDown();
        DFConfigManager.getInstance().shutDown();
    }

    public boolean isDebug() {
        return debug;
    }

    public void onLowMemory() {
    }

    public void onTrimMemory(int level) {

    }

    /**
     * 构造器
     */
    public static class Builder {
        private Context context;
        private String appkey;
        private String appSecret;
        private String pkgName;
        private String appVer;
        private String path;
        private short heartBeatInterval;
        private boolean isDebug;
        private MethodValueAopHandler handler;
        private boolean level;
        private String prefix;
        private MLog.LogInterface logInterface;
        private String url;
        private int port = -1;
        private int[] types;

        public static Builder newBuilder() {
            return new Builder();
        }


        public Builder isDebugEnalbe(boolean isDebug) {
            this.isDebug = isDebug;
            return this;
        }

        public Builder context(Context context) {
            this.context = context;
            return this;
        }

        public Builder appKey(String appkey, String appSecret) {
            this.appkey = appkey;
            this.appSecret = appSecret;
            return this;
        }

        public Builder appInfo(String pkgName, String appVer) {
            this.pkgName = pkgName;
            this.appVer = appVer;
            return this;
        }

        public Builder uploadService(String url, int port, String cachePath, short heartBeatInterval) {
            if (EmptyUtils.isEmpty(url)) {
                throw new IllegalArgumentException("url can't be null or empty");
            }
            if (port < 0 || port > 66000) {
                throw new IllegalArgumentException("port can't be " + port);
            }
            this.url = url;
            this.port = port;
            this.path = cachePath;
            this.heartBeatInterval = heartBeatInterval;
            return this;
        }

        public Builder mAopHandler(MethodValueAopHandler handler) {
            this.handler = handler;
            return this;
        }

        public Builder MLog(boolean level, String prefix, MLog.LogInterface logInterface) {
            this.level = level;
            this.logInterface = logInterface;
            this.prefix = prefix;
            return this;
        }

        public Builder install(int[] types) {
            this.types = types;
            return this;
        }

        public MMonitor build() {

            if (EmptyUtils.isEmpty(url)) {
                throw new IllegalArgumentException("url can't be null or empty");
            }

            if (port < -1 || port > 66000) {
                throw new IllegalArgumentException("port can't be " + port);
            }


            Monitor.Builder.newBuilder()
                    .context(context)
                    .isDebugEnalbe(isDebug)
                    .uploadService(MUploadService.getInstance())
                    .configManager(DFConfigManager.getInstance())
                    .MLog(level, prefix, logInterface)
                    .mAopHandler(handler)
                    .install(types)
                    .build();


            //生成初始化数据
            MUploadService.Info info = new MUploadService.Info(context, MasData.getDeviceId(context)
                    , pkgName, appkey, appSecret, appVer, BuildConfig.SDK_VER, path,
                    heartBeatInterval, url, port);

            msInstance = new MMonitor(info, isDebug);

            return msInstance;
        }

    }


}
