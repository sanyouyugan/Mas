package com.qiudaoyu.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * 创建时间: 2018/12/14
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class RemoteService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("intent onBind", "" + intent.getParcelableExtra("abc"));
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("intent onStartCommand", "" + intent.getParcelableExtra("abc"));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }


    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }


}
