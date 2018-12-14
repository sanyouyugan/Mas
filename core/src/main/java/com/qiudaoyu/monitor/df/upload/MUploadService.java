package com.qiudaoyu.monitor.df.upload;

import android.content.Context;

import com.qiudaoyu.monitor.upload.UploadServiceInterface;

/**
 * 创建时间: 2018/9/26
 * 类描述:
 * 上传日志类
 *
 * @author 秋刀鱼
 * @version 1.0
 */

public class MUploadService implements UploadServiceInterface<MUploadService.Info> {

    private MUploadService() {

    }

    public static MUploadService getInstance() {
        return Single.mInstance;
    }

    @Override
    public void initialize(Info args) {

    }

    @Override
    public void sendData(Object data) {

    }

    @Override
    public void setForground(boolean isForgroud) {

    }

    @Override
    public void shutDown() {

    }


    public static class Info {
        public Context context;
        public String pkgName;
        public String deviceId;
        public String appkey;
        public String secret;
        public String appVer;
        public String path;
        public String sdkversion;
        public String url;
        public int port;
        public short heartBeatInterval;

        public Info(Context context, String deviceId, String pkgName, String appkey, String secret, String appVer, String sdkversion, String path, short heartBeatInterval
                , String url, int port) {
            this.context = context;
            this.pkgName = pkgName;
            this.deviceId = deviceId;
            this.appkey = appkey;
            this.secret = secret;
            this.appVer = appVer;
            this.path = path;
            this.sdkversion = sdkversion;
            this.heartBeatInterval = heartBeatInterval;
            this.url = url;
            this.port = port;
        }


    }


    private static class Single {
        static MUploadService mInstance = new MUploadService();
    }


}
