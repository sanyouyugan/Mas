package com.qiudaoyu.monitor.utils;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static com.qiudaoyu.monitor.utils.AppUtils.checkHasPermission;

public class DeviceUtils {
    private static final String SELECT_RUNTIME_PROPERTY = "persist.sys.dalvik.vm.lib";
    private static final String LIB_DALVIK = "libdvm.so";
    private static final String LIB_ART = "libart.so";
    private static final String LIB_ART_D = "libartd.so";

    private static final List<String> mInvalidAndroidId = new ArrayList<String>() {
        {
            add("9774d56d682e549c");
            add("0123456789abcdef");
        }
    };

    public static String getDeviceId(Context context) {

        String deviceId = "";
        try {
            // 检测权限
            if (AppUtils.checkHasPermission(context, android.Manifest.permission.READ_PHONE_STATE)) {
                //先获取Imei号
                deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            }
        } catch (Exception e) {
        }

        if (EmptyUtils.isNotEmpty(deviceId)) {
            if (deviceId.contains("+")) {
                deviceId = deviceId.replaceAll("\\+", "");
            }
        }

        //在获取mac地址
        try {
            HashMap<String, String> macs = NetWorkUtils.getMacsFromSystemFile();
            if (macs != null && !macs.isEmpty()) {
                List<String> keys = new ArrayList<>(macs.keySet());
                Collections.sort(keys, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.compareTo(o2);
                    }
                });
                for (String key : keys) {
                    deviceId += macs.get(key);
                }
            }
        } catch (Exception e) {
        }


        if (EmptyUtils.isEmpty(deviceId)) {
            //获取AndroidId
            deviceId = getAndroidID(context);
            if (EmptyUtils.isEmpty(deviceId) || mInvalidAndroidId.contains(deviceId)) {
                //UUID
                deviceId = new UUIDGenerator().generate();
            }
        }

        return MD5Utils.encode(deviceId);
    }

    /**
     * androidId是否有效
     *
     * @param androidId
     * @return
     */
    public static boolean isValidAndroidId(String androidId) {
        if (TextUtils.isEmpty(androidId)) {
            return false;
        }

        if (mInvalidAndroidId.contains(androidId.toLowerCase())) {
            return false;
        }

        return true;
    }

    /**
     * 获取 Android ID
     *
     * @param mContext Context
     * @return androidID
     */
    public static String getAndroidID(Context mContext) {
        String androidID = "";
        try {
            androidID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return androidID;
    }

    /**
     * 虚拟机模式
     *
     * @return
     */
    public static boolean getIsArtInUse() {
        final String vmVersion = System.getProperty("java.vm.version");
        return vmVersion != null && vmVersion.startsWith("2");
    }

    /**
     * 虚拟机模式
     *
     * @return
     */
    private CharSequence getCurrentRuntimeValue() {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            try {
                Method get = systemProperties.getMethod("get",
                        String.class, String.class);
                if (get == null) {
                    return "WTF?!";
                }
                try {
                    final String value = (String) get.invoke(
                            systemProperties, SELECT_RUNTIME_PROPERTY,
                            /* Assuming default is */"Dalvik");
                    if (LIB_DALVIK.equals(value)) {
                        return "Dalvik";
                    } else if (LIB_ART.equals(value)) {
                        return "ART";
                    } else if (LIB_ART_D.equals(value)) {
                        return "ART debug build";
                    }

                    return value;
                } catch (IllegalAccessException e) {
                    return "IllegalAccessException";
                } catch (IllegalArgumentException e) {
                    return "IllegalArgumentException";
                } catch (InvocationTargetException e) {
                    return "InvocationTargetException";
                }
            } catch (NoSuchMethodException e) {
                return "SystemProperties.get(String key, String def) method is not found";
            }
        } catch (ClassNotFoundException e) {
            return "SystemProperties class is not found";
        }
    }
}
