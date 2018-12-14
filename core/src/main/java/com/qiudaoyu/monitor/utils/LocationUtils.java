package com.qiudaoyu.monitor.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;

import com.qiudaoyu.monitor.log.MLog;

import java.util.List;

import static android.content.Context.LOCATION_SERVICE;

/**
 * 创建时间: 2018/12/5
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class LocationUtils {

    private volatile boolean isWorking;
    private volatile LocationListener locationListener;
    private volatile Location lastLocation;

    private LocationUtils() {

    }

    public static LocationUtils getInstace() {
        return Single.sInstace;
    }

    /**
     * 获取定位数据,gps不能一次定位成功，还需
     *
     * @param context
     * @param locationListener
     * @return
     */
    public void initLocation(Context context, LocationListener locationListener) {

        LocationManager locationManager;
        try {
            //获取定位服务
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            //获取当前可用的位置控制器
            List<String> list = locationManager.getProviders(true);
            String provider = null;
            if (list.contains(LocationManager.GPS_PROVIDER)) {
                //是否为GPS位置控制器
                provider = LocationManager.GPS_PROVIDER;
            } else if (list.contains(LocationManager.NETWORK_PROVIDER)) {
                //是否为网络位置控制器
                provider = LocationManager.NETWORK_PROVIDER;
            }

            if (provider == null) {
                return;
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //没有权限
                return;
            }
            Location location = getLastKnownLocation(context, locationManager);
            if (!isWorking || lastLocation == null) {
                try {
                    locationManager.requestLocationUpdates(provider, 30000, 50,
                            locationListener, Looper.getMainLooper());
                    isWorking = true;
                    this.locationListener = locationListener;
                } catch (Exception e) {
                    try {
                        if (locationManager != null) {
                            locationManager.removeUpdates(locationListener);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
            lastLocation = location;
        } catch (Exception e) {
            //获取位置失败
            MLog.e("loc", "fail", e);
        }
    }

    /**
     * 获取gps
     *
     * @param context
     * @param mLocationManager
     * @return
     */
    private Location getLastKnownLocation(Context context, LocationManager mLocationManager) {
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //没有权限
            return null;
        }

        for (String provider : providers) {
            try {
                Location l = mLocationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            } catch (Exception e) {
            }
        }
        return bestLocation;

    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    /**
     * 结束问题
     *
     * @param context
     */
    public void shutDown(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        try {
            if (isWorking) {
                if (locationListener != null) {
                    locationManager.removeUpdates(locationListener);
                }
                locationListener = null;
                isWorking = false;
                lastLocation = null;
            }

        } catch (Exception e) {
        } finally {
            locationListener = null;
            isWorking = false;
            lastLocation = null;
        }
    }

    public boolean isWorking() {
        return isWorking;
    }

    private static class Single {
        static LocationUtils sInstace;

        static {
            sInstace = new LocationUtils();
        }
    }
}
