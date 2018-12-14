package com.qiudaoyu.monitor.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.qiudaoyu.monitor.utils.AppUtils.checkHasPermission;

/**
 * 网络状态工具类
 */
public class NetWorkUtils {

    /**
     * 高通芯片判断
     *
     * @param mContext
     * @return
     */
    private static final Integer SIMID_1 = 0;
    private static final Integer SIMID_2 = 1;
    private static final Map<String, String> sCarrierMap = new HashMap<String, String>() {
        {
            //中国移动
            put("46000", "中国移动");
            put("46002", "中国移动");
            put("46007", "中国移动");
            put("46008", "中国移动");

            //中国联通
            put("46001", "中国联通");
            put("46006", "中国联通");
            put("46009", "中国联通");

            //中国电信
            put("46003", "中国电信");
            put("46005", "中国电信");
            put("46011", "中国电信");

            //中国卫通
            put("46004", "中国卫通");

            //中国铁通
            put("46020", "中国铁通");

        }
    };
    private static Set<String> charSet = new HashSet<>();

    static {
        charSet.add("0");
        charSet.add("1");
        charSet.add("2");
        charSet.add("3");
        charSet.add("4");
        charSet.add("5");
        charSet.add("6");
        charSet.add("7");
        charSet.add("8");
        charSet.add("9");
        charSet.add("a");
        charSet.add("b");
        charSet.add("c");
        charSet.add("d");
        charSet.add("e");
        charSet.add("f");
    }

    /**
     * ping 操作
     *
     * @param host
     * @param pingCount
     * @param stringBuffer
     * @return
     * @throws Exception
     */
    public static boolean ping(String host, int pingCount, StringBuffer stringBuffer) throws Exception {
        String line;
        Process process = null;
        BufferedReader successReader = null;
        //ping -c %d -i %d host
        StringBuilder commandBuild = new StringBuilder();
        commandBuild.append("ping ").
                append("-c " + String.valueOf(pingCount) + " ")
                .append("-i " + String.valueOf(0.5) + " ")
                .append(host);

        String command = commandBuild.toString();
        boolean isSuccess = false;
        try {
            try {
                process = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                throw e;
            }
            if (process == null) {
                append(stringBuffer, "ping fail:process is null.");
                return false;
            }
            successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = successReader.readLine()) != null) {
                append(stringBuffer, line);
            }
            int status = process.waitFor();
            if (status == 0) {
                append(stringBuffer, "exec cmd end:" + command);
                isSuccess = true;
            } else {
                append(stringBuffer, "exec cmd fail.");
                isSuccess = false;
            }
            append(stringBuffer, "exec finished.");
        } catch (IOException e) {
            Log.d("ping", "" + e.getLocalizedMessage());
        } catch (InterruptedException e) {
            Log.d("ping", "" + e.getLocalizedMessage());
        } finally {
            try {
                process.getInputStream().close();
                process.getOutputStream().close();
                process.getErrorStream().close();
            } catch (Exception e) {
            }
            try {
                process.destroy();
            } catch (Exception e) {
            }
            if (successReader != null) {
                try {
                    successReader.close();
                } catch (IOException e) {
                }
            }
        }
        return isSuccess;
    }

    private static void append(StringBuffer stringBuffer, String text) {
        if (stringBuffer != null) {
            stringBuffer.append(text + "\n");
        }
    }

    /**
     * 从文件系统获取mac
     *
     * @return
     * @throws Exception
     */
    public static HashMap<String, String> getMacsFromSystemFile() {
        try {
            HashMap<String, String> hashMap = new HashMap<>();
            File netFiles = new File("/sys/class/net");
            for (File file : netFiles.listFiles()) {
                if (file.getName().startsWith("wlan")
                        || file.getName().startsWith("eth")) {
                    File addressFile = new File("/sys/class/net/" + file.getName() + "/address");
                    LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(addressFile), "UTF-8"));
                    StringBuilder mac = new StringBuilder();
                    for (char cha : reader.readLine().toLowerCase().toCharArray()) {
                        if (charSet.contains(new String("" + cha))) {
                            mac.append(cha);
                        }
                    }
                    if (mac.length() == 12 && !"000000000000".equals(mac.toString())) {
                        hashMap.put(file.getName(), mac.toString());
                    }
                    reader.close();
                }
            }
            return hashMap;
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 系统api获取wifi的mac地址
     *
     * @return
     */
    private static String getMacByApi() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (nif.getName().equalsIgnoreCase("wlan0")) {
                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }

                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:", b));
                    }

                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            }

        } catch (Exception e) {
            //ignore
        }
        return null;
    }

    /**
     * 获取本地IP
     *
     * @return
     */
    public static String getLocalIp() {
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface
                    .getNetworkInterfaces(); enNetI.hasMoreElements(); ) {
                NetworkInterface netI = enNetI.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = netI
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress() == null ? "" :
                                inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取IMSInfo,包括双卡
     *
     * @return
     */
    public static IMSInfo getIMSInfoAll(Context mContext) {
        // 检测权限
        if (!AppUtils.checkHasPermission(mContext, android.Manifest.permission.READ_PHONE_STATE)) {
            return null;
        }
        IMSInfo imsInfo = initQualcommDoubleSim(mContext);
        if (imsInfo != null) {
            return imsInfo;
        } else {
            imsInfo = initMtkDoubleSim(mContext);
            if (imsInfo != null) {
                return imsInfo;
            } else {
                imsInfo = initMtkSecondDoubleSim(mContext);
                if (imsInfo != null) {
                    return imsInfo;
                } else {
                    imsInfo = initSpreadDoubleSim(mContext);
                    if (imsInfo != null) {
                        return imsInfo;
                    } else {
                        imsInfo = getIMSI(mContext);
                        if (imsInfo != null) {
                            return imsInfo;
                        } else {
                            imsInfo = null;
                            return imsInfo;
                        }
                    }
                }
            }
        }
    }

    /**
     * MTK的芯片的判断
     *
     * @param mContext
     * @return
     */
    public static IMSInfo initMtkDoubleSim(Context mContext) {
        // 检测权限
        if (!AppUtils.checkHasPermission(mContext, android.Manifest.permission.READ_PHONE_STATE)) {
            return null;
        }

        IMSInfo imsInfo;
        try {
            TelephonyManager tm = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            Class<?> c = Class.forName("com.android.internal.telephony.Phone");
            Field fields1 = c.getField("GEMINI_SIM_1");
            fields1.setAccessible(true);
            Integer simId_1 = (Integer) fields1.get(null);
            Field fields2 = c.getField("GEMINI_SIM_2");
            fields2.setAccessible(true);
            Integer simId_2 = (Integer) fields2.get(null);

            Method m = TelephonyManager.class.getDeclaredMethod(
                    "getSubscriberIdGemini", int.class);
            String imsi_1 = (String) m.invoke(tm, simId_1);
            String imsi_2 = (String) m.invoke(tm, simId_2);

            Method m1 = TelephonyManager.class.getDeclaredMethod(
                    "getDeviceIdGemini", int.class);
            String imei_1 = (String) m1.invoke(tm, simId_1);
            String imei_2 = (String) m1.invoke(tm, simId_2);

            imsInfo = new IMSInfo();
            imsInfo.chipName = "MTK芯片";
            imsInfo.imei_1 = imei_1;
            imsInfo.imei_2 = imei_2;
            imsInfo.imsi_1 = imsi_1;
            imsInfo.imsi_2 = imsi_2;

        } catch (Exception e) {
            imsInfo = null;
            return imsInfo;
        }
        return imsInfo;
    }

    /**
     * 高通双卡
     *
     * @param mContext
     * @return
     */
    public static IMSInfo initQualcommDoubleSim(Context mContext) {
        // 检测权限
        if (!AppUtils.checkHasPermission(mContext, android.Manifest.permission.READ_PHONE_STATE)) {
            return null;
        }

        IMSInfo imsInfo = null;
        try {
            Class<?> cx = Class.forName("android.telephony.MSimTelephonyManager");
            @SuppressLint("WrongConstant") Object obj = mContext.getSystemService("phone_msim");
            Method md = cx.getMethod("getDeviceId", int.class);
            Method ms = cx.getMethod("getSubscriberId", int.class);
            String imei_1 = (String) md.invoke(obj, SIMID_1);
            String imei_2 = (String) md.invoke(obj, SIMID_2);
            String imsi_1 = (String) ms.invoke(obj, SIMID_1);
            String imsi_2 = (String) ms.invoke(obj, SIMID_2);
            int statephoneType_2 = 0;
            boolean flag = false;
            try {
                Method mx = cx.getMethod("getPreferredDataSubscription", int.class);
                Method is = cx.getMethod("isMultiSimEnabled", int.class);
                statephoneType_2 = (Integer) mx.invoke(obj);
                flag = (Boolean) is.invoke(obj);
            } catch (Exception e) {
                // TODO: handle exception
            }
            imsInfo = new IMSInfo();
            imsInfo.chipName = "高通芯片-getPreferredDataSubscription:" + statephoneType_2 + ",\nflag:" + flag;
            imsInfo.imei_1 = imei_1;
            imsInfo.imei_2 = imei_2;
            imsInfo.imsi_1 = imsi_1;
            imsInfo.imsi_2 = imsi_2;

        } catch (Exception e) {
            imsInfo = null;
            return imsInfo;
        }
        return imsInfo;
    }

    /**
     * 系统的api
     *
     * @return
     */
    public static IMSInfo getIMSI(Context mContext) {
        // 检测权限
        if (!AppUtils.checkHasPermission(mContext, android.Manifest.permission.READ_PHONE_STATE)) {
            return null;
        }

        IMSInfo imsInfo = null;
        String imsi_1;
        String imei_1;
        try {
            TelephonyManager tm = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            imsi_1 = tm.getSubscriberId();
            imei_1 = tm.getDeviceId();
        } catch (Exception e) {
            // TODO: handle exception
            imsInfo = null;
            return imsInfo;
        }
        if (EmptyUtils.isEmpty(imsi_1) || imsi_1.length() < 10) {
            imsInfo = null;
            return imsInfo;
        } else {
            imsInfo = new IMSInfo();
            imsInfo.chipName = "单卡芯片";
            imsInfo.imei_1 = imei_1;
            imsInfo.imei_2 = "没有";
            imsInfo.imsi_1 = imsi_1;
            imsInfo.imsi_2 = "没有";
            return imsInfo;
        }
    }

    /**
     * MTK的芯片的判断2
     *
     * @param mContext
     * @return
     */

    public static IMSInfo initMtkSecondDoubleSim(Context mContext) {
        // 检测权限
        if (!AppUtils.checkHasPermission(mContext, android.Manifest.permission.READ_PHONE_STATE)) {
            return null;
        }
        IMSInfo imsInfo = null;
        try {
            TelephonyManager tm = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            Class<?> c = Class.forName("com.android.internal.telephony.Phone");
            Field fields1 = c.getField("GEMINI_SIM_1");
            fields1.setAccessible(true);
            Integer simId_1 = (Integer) fields1.get(null);
            Field fields2 = c.getField("GEMINI_SIM_2");
            fields2.setAccessible(true);
            Integer simId_2 = (Integer) fields2.get(null);

            Method mx = TelephonyManager.class.getMethod("getDefault",
                    int.class);
            TelephonyManager tm1 = (TelephonyManager) mx.invoke(tm, simId_1);
            TelephonyManager tm2 = (TelephonyManager) mx.invoke(tm, simId_2);

            String imsi_1 = tm1.getSubscriberId();
            String imsi_2 = tm2.getSubscriberId();

            String imei_1 = tm1.getDeviceId();
            String imei_2 = tm2.getDeviceId();

            imsInfo = new IMSInfo();
            imsInfo.chipName = "MTK芯片";
            imsInfo.imei_1 = imei_1;
            imsInfo.imei_2 = imei_2;
            imsInfo.imsi_1 = imsi_1;
            imsInfo.imsi_2 = imsi_2;

        } catch (Exception e) {
            imsInfo = null;
            return imsInfo;
        }
        return imsInfo;
    }

    /**
     * 展讯芯片的判断
     *
     * @param mContext
     * @return
     */

    public static IMSInfo initSpreadDoubleSim(Context mContext) {
        // 检测权限
        if (!AppUtils.checkHasPermission(mContext, android.Manifest.permission.READ_PHONE_STATE)) {
            return null;
        }
        IMSInfo imsInfo;
        try {
            Class<?> c = Class
                    .forName("com.android.internal.telephony.PhoneFactory");
            Method m = c.getMethod("getServiceName", String.class, int.class);
            String spreadTmService = (String) m.invoke(c,
                    Context.TELEPHONY_SERVICE, 1);
            TelephonyManager tm = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String imsi_1 = tm.getSubscriberId();
            String imei_1 = tm.getDeviceId();
            TelephonyManager tm1 = (TelephonyManager) mContext
                    .getSystemService(spreadTmService);
            String imsi_2 = tm1.getSubscriberId();
            String imei_2 = tm1.getDeviceId();
            imsInfo = new IMSInfo();
            imsInfo.chipName = "展讯芯片";
            imsInfo.imei_1 = imei_1;
            imsInfo.imei_2 = imei_2;
            imsInfo.imsi_1 = imsi_1;
            imsInfo.imsi_2 = imsi_2;
        } catch (Exception e) {
            imsInfo = null;
            return imsInfo;
        }
        return imsInfo;
    }

    /**
     * 网络类型
     * wifi,2g,3g,4g
     *
     * @param context
     * @return
     */
    public static String networkType(Context context) {
        // 检测权限
        if (!AppUtils.checkHasPermission(context, "android.permission.ACCESS_NETWORK_STATE")) {
            return "NULL";
        }

        // Wifi
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            NetworkInfo networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                return "WIFI";
            }
        }

        // Mobile network
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context
                .TELEPHONY_SERVICE);

        int networkType = telephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
        }

        // disconnected to the internet
        return "NULL";
    }

    /**
     * 网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        // 检测权限
        if (!AppUtils.checkHasPermission(context, "android.permission.ACCESS_NETWORK_STATE")) {
            return false;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 单卡获取IMEI
     *
     * @param mContext Context
     * @return IMEI
     */
    public static String getIMEI(Context mContext) {
        String imei = "";
        try {
            if (ContextCompat.checkSelfPermission(mContext, "android.permission.READ_PHONE_STATE") != PackageManager.PERMISSION_GRANTED) {
                return imei;
            }
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                imei = tm.getDeviceId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imei;
    }

    public static String operatorToCarrier(Context context, String operator) {
        final String other = "其他";

        try {
            if (TextUtils.isEmpty(operator)) {
                return other;
            }

            for (Map.Entry<String, String> entry : sCarrierMap.entrySet()) {
                if (operator.startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }

            String carrierJson = getJsonFromAssets("mcc_mnc_mini.json", context);
            if (TextUtils.isEmpty(carrierJson)) {
                return other;
            }

            JSONObject jsonObject = new JSONObject(carrierJson);
            int operatorLength = operator.length();
            String carrier = null;

            //mcc与mnc之和为5位数或6位数,6位数比较少，先截取6位数进行判断
            if (operatorLength >= 6) {
                String mccMnc = operator.substring(0, 6);
                carrier = getCarrierFromJsonObject(jsonObject, mccMnc);
            }

            if (TextUtils.isEmpty(carrier) && operatorLength >= 5) {
                String mccMnc = operator.substring(0, 5);
                carrier = getCarrierFromJsonObject(jsonObject, mccMnc);
            }

            if (!TextUtils.isEmpty(carrier)) {
                return carrier;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return other;
    }

    private static String getJsonFromAssets(String fileName, Context context) {
        //将json数据变成字符串
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bf = null;
        try {
            //获取assets资源管理器
            AssetManager assetManager = context.getAssets();
            //通过管理器打开文件并读取
            bf = new BufferedReader(new InputStreamReader(
                    assetManager.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bf != null) {
                try {
                    bf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuilder.toString();
    }

    /**
     *
     * 需要优化一下
     *
     *
     * @param context
     * @return
     */
    public static String getCarrier(Context context) {
        try {
            if (AppUtils.checkHasPermission(context, "android.permission.READ_PHONE_STATE")) {
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context
                            .TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        String operatorString = telephonyManager.getSubscriberId();
                        if (!TextUtils.isEmpty(operatorString)) {
                            return operatorToCarrier(context, operatorString);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private static String getCarrierFromJsonObject(JSONObject jsonObject, String mccMnc) {
        if (jsonObject == null || TextUtils.isEmpty(mccMnc)) {
            return null;
        }
        return jsonObject.optString(mccMnc);

    }

    /**
     * imsi对象
     */
    public static class IMSInfo {
        public String chipName;
        public String imsi_1;
        public String imei_1;
        public String imsi_2;
        public String imei_2;

        public String getChipName() {
            return chipName;
        }

        public void setChipName(String chipName) {
            this.chipName = chipName;
        }

        public String getImsi_1() {
            return imsi_1;
        }

        public void setImsi_1(String imsi_1) {
            this.imsi_1 = imsi_1;
        }

        public String getImei_1() {
            return imei_1;
        }

        public void setImei_1(String imei_1) {
            this.imei_1 = imei_1;
        }

        public String getImsi_2() {
            return imsi_2;
        }

        public void setImsi_2(String imsi_2) {
            this.imsi_2 = imsi_2;
        }

        public String getImei_2() {
            return imei_2;
        }

        public void setImei_2(String imei_2) {
            this.imei_2 = imei_2;
        }

        @Override
        public String toString() {
            return "{" +
                    "chipName='" + chipName + '\'' +
                    ", \nimsi_1='" + imsi_1 + '\'' +
                    ", \nimei_1='" + imei_1 + '\'' +
                    ", \nimsi_2='" + imsi_2 + '\'' +
                    ", \nimei_2='" + imei_2 + '\'' +
                    '}';
        }
    }

}
