package com.qiudaoyu.monitor.utils;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 创建时间: 2017/8/28 上午11:19
 * 类描述:
 *
 * @author 木棉
 */

public class MD5Utils {

    private static final String[] HEX_DIGITS = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    private MD5Utils() {
    }

    private static String byteArrayToHexString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (byte aB : b) {
            buf.append(byteToHexString(aB));
        }
        return buf.toString();
    }

    private static String byteToHexString(byte b) {
        return HEX_DIGITS[(b & 240) >> 4] + HEX_DIGITS[b & 15];
    }

    public static String encode(String origin) {
        if (origin == null) {
            throw new IllegalArgumentException("MULTI_000523");
        } else {
            String resultString = origin;
            try {
                MessageDigest e = MessageDigest.getInstance("MD5");
                try {
                    resultString = byteArrayToHexString(e.digest(resultString.getBytes("UTF-8")));
                } catch (UnsupportedEncodingException var4) {
                    var4.printStackTrace();
                }
                return resultString;
            } catch (NoSuchAlgorithmException var5) {
                return null;
            }
        }
    }
    /**
     * 对输入流生成校验码.
     * @param in 输入流.
     * @return 生成的校验码.
     */
    public static String encode(InputStream in) {
        if (in == null) {
            throw new NullPointerException("origin == null");
        }
        String resultString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024 * 1024];
            int len = 0;
            while ((len = in.read(buffer)) > 0) {
                md.update(buffer, 0, len);
            }
            resultString = byteArrayToHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e("MD5Util","NoSuchAlgorithmException",e);
        } catch (IOException e) {
            Log.e("MD5Util","InputStream read error",e);
        }
        return resultString;
    }
}

