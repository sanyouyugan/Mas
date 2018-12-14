package com.qiudaoyu.monitor.analysis.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * 创建时间: 2018/12/4
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class MasUtils {
    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 合并 JSONObject
     *
     * @param source JSONObject
     * @param dest   JSONObject
     * @throws JSONException Exception
     */

    public static void mergeJSONObject(final JSONObject source, JSONObject dest) {
        try {
            Iterator<String> superPropertiesIterator = source.keys();
            while (superPropertiesIterator.hasNext()) {
                String key = superPropertiesIterator.next();
                Object value = source.get(key);
                if (value instanceof Date) {
                    synchronized (mDateFormat) {
                        dest.put(key, mDateFormat.format((Date) value));
                    }
                } else {
                    dest.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
