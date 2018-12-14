package com.qiudaoyu.monitor.analysis.activity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiudaoyu on 2016/11/30
 * ExpandableListView
 */

public interface MasExpandableListViewItemTrackProperties {
    /**
     * 点击 groupPosition、childPosition 处 item 的扩展属性
     * @param groupPosition
     * @param childPosition
     * @return
     * @throws JSONException
     */
    JSONObject getChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    /**
     * 点击 groupPosition 处 item 的扩展属性
     * @param groupPosition
     * @return
     * @throws JSONException
     */
    JSONObject getGroupItemTrackProperties(int groupPosition) throws JSONException;
}