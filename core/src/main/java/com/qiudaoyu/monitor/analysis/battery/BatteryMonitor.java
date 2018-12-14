package com.qiudaoyu.monitor.analysis.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.log.MLog;

/**
 * 创建时间: 2018/12/2
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class BatteryMonitor extends MetricMonitor {
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent batteryInfoIntent) {
            BatteryInfo batteryInfo = new BatteryInfo();
            batteryInfo.status = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager
                    .BATTERY_STATUS_UNKNOWN);
            batteryInfo.health = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager
                    .BATTERY_HEALTH_UNKNOWN);
            batteryInfo.present = batteryInfoIntent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false);
            batteryInfo.level = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batteryInfo.scale = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
            batteryInfo.plugged = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            batteryInfo.voltage = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            batteryInfo.temperature = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            batteryInfo.technology = batteryInfoIntent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
            batteryInfo.time = System.currentTimeMillis();
            metricData(MonitorData.TYPE_METRIC_BATTERY, batteryInfo);
        }
    };

    public BatteryMonitor(MetricListener metricListener) {
        super(metricListener);
    }

    /**
     * 获取电池信息
     *
     * @param context
     * @return 获取电池信息
     */
    public void start(Context context) {
        try {
            IntentFilter BATTERY_INTENT_FILTER = new IntentFilter();
            BATTERY_INTENT_FILTER.addAction(Intent.ACTION_BATTERY_CHANGED);
            BATTERY_INTENT_FILTER.addAction(Intent.ACTION_BATTERY_LOW);
            BATTERY_INTENT_FILTER.addAction(Intent.ACTION_BATTERY_OKAY);
            context.registerReceiver(batteryReceiver, BATTERY_INTENT_FILTER);
        } catch (Throwable e) {
            MLog.e("Battry", "fail ", e);
        }
    }

    public void stop(Context context) {
        if (context != null) {
            try {
                context.unregisterReceiver(batteryReceiver);
            } catch (Exception e) {
            }
        }
    }

}
