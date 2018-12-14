package com.qiudaoyu.monitor.analysis.activity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiudaoyu on 2016/11/30
 * 获取 ListView、GridView position 位置 Item 的 properties
 */

public interface MasAdapterViewItemTrackProperties {
    /**
     * 点击 position 处 item 的扩展属性
     *
     * @param position 当前 item 所在位置
     * @return JSONObject
     * @throws JSONException JSON 异常
     */
    JSONObject getItemTrackProperties(int position) throws JSONException;
}