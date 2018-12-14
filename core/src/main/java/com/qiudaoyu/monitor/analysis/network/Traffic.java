package com.qiudaoyu.monitor.analysis.network;

/**
 * 应用的流量信息
 * 6.0以下
 * TrafficStats
 * https://blog.csdn.net/u013205623/article/details/52860071
 * <p>
 * 6.0以上
 * NetworkStatsHistory
 * https://blog.csdn.net/WDYShowTime/article/details/78532182
 * https://blog.csdn.net/w7849516230/article/details/71705835
 */

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.TrafficStats;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.qiudaoyu.monitor.log.MLog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;

import static android.content.Context.NETWORK_STATS_SERVICE;

public class Traffic implements Serializable {

    /**
     * 不支持状态【标识变量】
     */
    private static final int UNSUPPORT = -1;
    /**
     * 打印信息标志
     */
    private static final String TAG = "TrafficBeforeM";

    /**
     * NetworkStatsManager的使用需要额外的权限，”android.permission.PACKAGE_USAGE_STATS”是系统权限，
     * 需要主动引导用户开启应用的“有权查看使用情况的应用”（使用记录访问权限）权限
     *
     * @param context
     * @param uid
     * @param netType   ConnectivityManager.TYPE_WIFI
     * @param startTime
     * @param endTime
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static TrafficBean getRecAndSendM(Context context, int uid, int netType, long startTime, long endTime) {
        try {
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(NETWORK_STATS_SERVICE);
            NetworkStats summaryStats;
            long summaryRx = 0;
            long summaryTx = 0;
            NetworkStats.Bucket summaryBucket = new NetworkStats.Bucket();

            summaryStats = networkStatsManager.queryDetailsForUid(netType, "", startTime, endTime, uid);
            do {
                summaryStats.getNextBucket(summaryBucket);
                int summaryUid = summaryBucket.getUid();
                if (uid == summaryUid) {
                    summaryRx += summaryBucket.getRxBytes();
                    summaryTx += summaryBucket.getTxBytes();
                }
            } while (summaryStats.hasNextBucket());
            return new TrafficBean(summaryTx, summaryRx, netType, (endTime - startTime));
        } catch (Exception e) {
            MLog.w(TAG, "getRecAndSendM ", e);
        }
        return null;
    }

    /**
     * 获取当前UID的总的上传流量
     *
     * @return
     */
    private long getRecAndSend(int uid) {
        long sendTraffic = UNSUPPORT;
        sendTraffic = TrafficStats.getUidTxBytes(uid);
        if (sendTraffic == UNSUPPORT) {
            return UNSUPPORT;
        }
        RandomAccessFile rafSend = null;
        String sndPath = "/proc/uid_stat/" + uid + "/tcp_snd";
        try {
            rafSend = new RandomAccessFile(sndPath, "r");
            sendTraffic = Long.parseLong(rafSend.readLine());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException: " + e.getMessage());
            sendTraffic = UNSUPPORT;
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rafSend != null)
                    rafSend.close();
            } catch (IOException e) {
                Log.w(TAG, "Close RandomAccessFile exception: " + e.getMessage());
            }
        }
        return sendTraffic;
    }

    /**
     * 获取当前UID的总的下载流量
     * 某个应用的网络流量数据保存在系统的
     * /proc/uid_stat/$UID/tcp_rcv | tcp_snd文件中
     *
     * @return
     */
    private long getRecTraffic(int uid) {
        long recTraffic = UNSUPPORT;
        recTraffic = TrafficStats.getUidRxBytes(uid);
        if (recTraffic == UNSUPPORT) {
            return UNSUPPORT;
        }
        Log.i(TAG, recTraffic + " ---1");
        //访问数据文件
        RandomAccessFile rafRec = null;
        String rcvPath = "/proc/uid_stat/" + uid + "/tcp_rcv";
        try {
            rafRec = new RandomAccessFile(rcvPath, "r");
            recTraffic = Long.parseLong(rafRec.readLine()); // 读取流量统计
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException: " + e.getMessage());
            recTraffic = UNSUPPORT;
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rafRec != null)
                    rafRec.close();
            } catch (IOException e) {
                Log.w(TAG, "Close RandomAccessFile exception: " + e.getMessage());
            }
        }
        Log.i("test", recTraffic + "--2");
        return recTraffic;
    }


    /**
     * 获取时间间隔类的网络流量
     */
    public static class TrafficBean implements Serializable {
        long sendByte;
        long recByte;
        int netType;
        long interval;

        public TrafficBean(long sendByte, long recByte, int netType, long interval) {
            this.sendByte = sendByte;
            this.recByte = recByte;
            this.netType = netType;
            this.interval = interval;
        }
    }
}
