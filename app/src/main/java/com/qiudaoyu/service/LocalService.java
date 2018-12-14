package com.qiudaoyu.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * 创建时间: 2018/12/14
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class LocalService extends Service {
    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        DataParcelable pa = intent.getParcelableExtra("xxxx");
        Log.d("", "onBind pa " + pa);
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     *
     *
     *
     */
    public class LocalBinder extends Binder {
        public LocalService getService() {
            return LocalService.this;
        }

    }

}
