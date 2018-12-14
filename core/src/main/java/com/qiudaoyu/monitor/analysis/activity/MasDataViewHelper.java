package com.qiudaoyu.monitor.analysis.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.qiudaoyu.monitor.Monitor;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.R;
import com.qiudaoyu.monitor.analysis.MasData;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;

import static com.qiudaoyu.monitor.analysis.utils.MasUtils.mergeJSONObject;
import static com.qiudaoyu.monitor.utils.AppUtils.getActivityFromContext;
import static com.qiudaoyu.monitor.utils.AppUtils.getActivityTitle;

@SuppressWarnings("unused")
public class MasDataViewHelper {


    static long startTime;
    static String eventName;
    static JSONObject Clickproperties;
    private static HashMap<Integer, Long> eventTimestamp = new HashMap<>();

    private static boolean isDeBounceTrack(Object object) {
        boolean isDeBounceTrack = false;
        long currentOnClickTimestamp = System.currentTimeMillis();
        Object targetObject = eventTimestamp.get(object.hashCode());
        if (targetObject != null) {
            long lastOnClickTimestamp = (long) targetObject;
            if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                isDeBounceTrack = true;
            }
        }

        eventTimestamp.put(object.hashCode(), currentOnClickTimestamp);
        return isDeBounceTrack;
    }

    private static void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName)) {
                return;
            }

            if (root == null) {
                return;
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                if (child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup) {
                    child.setTag(R.id.mas_tag_view_fragment_name, fragmentName);
                } else if (child instanceof ViewGroup) {
                    traverseView(fragmentName, (ViewGroup) child);
                } else {
                    child.setTag(R.id.mas_tag_view_fragment_name, fragmentName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * fragmentView创建
     *
     * @param object
     * @param rootView
     * @param bundle
     */
    public static void onFragmentViewCreated(Object object, View rootView, Bundle bundle) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {
            if (!(object instanceof android.support.v4.app.Fragment)) {
                return;
            }

            android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) object;
            //Fragment名称
            String fragmentName = fragment.getClass().getName();

            if (rootView instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) rootView);
            } else {
                rootView.setTag(R.id.mas_tag_view_fragment_name, fragmentName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * rn
     *
     * @param target
     * @param reactTag
     * @param s
     * @param b
     */
    public static void trackRN(Object target, int reactTag, int s, boolean b) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {

            JSONObject properties = new JSONObject();
            properties.put(MasData.ELEMENT_TYPE, "RNView");
            if (target != null) {
                Class<?> clazz = Class.forName("com.facebook.react.uimanager.NativeViewHierarchyManager");
                Method resolveViewMethod = clazz.getMethod("resolveView", int.class);
                if (resolveViewMethod != null) {
                    Object object = resolveViewMethod.invoke(target, reactTag);
                    if (object != null) {
                        View view = (View) object;
                        //获取所在的 Context
                        Context context = view.getContext();

                        //将 Context 转成 Activity
                        Activity activity = getActivityFromContext(context, view);
                        //$screen_name & $title
                        if (activity != null) {
                            properties.put(MasData.SCREEN_NAME, activity.getClass().getCanonicalName());
                            String activityTitle = getActivityTitle(activity);
                            if (!TextUtils.isEmpty(activityTitle)) {
                                properties.put(MasData.TITLE, activityTitle);
                            }
                        }
                        if (view instanceof CompoundButton) {//ReactSwitch
                            return;
                        }
                        if (view instanceof TextView) {
                            TextView textView = (TextView) view;
                            if (!TextUtils.isEmpty(textView.getText())) {
                                properties.put(MasData.ELEMENT_CONTENT, textView.getText().toString());
                            }
                        } else if (view instanceof ViewGroup) {
                            StringBuilder stringBuilder = new StringBuilder();
                            String viewText = traverseView(stringBuilder, (ViewGroup) view);
                            if (!TextUtils.isEmpty(viewText)) {
                                viewText = viewText.substring(0, viewText.length() - 1);
                            }
                            properties.put(MasData.ELEMENT_CONTENT, viewText);
                        }
                    }
                }
            }
            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fragment
     */
    private static void trackFragmentAppViewScreen(android.support.v4.app.Fragment fragment) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragment.getClass().getCanonicalName())) {
                return;
            }

            if (fragment.getClass().getAnnotation(MasDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            JSONObject properties = new JSONObject();

            Activity activity = fragment.getActivity();
            String fragmentName = fragment.getClass().getCanonicalName();
            if (activity != null) {
                String activityTitle = getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(MasData.TITLE, activityTitle);
                }
                properties.put(MasData.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName));
            } else {
                properties.put(MasData.SCREEN_NAME, fragmentName);
            }

            MasDataFragmentTitle sensorsDataFragmentTitle = fragment.getClass().getAnnotation(MasDataFragmentTitle.class);
            if (sensorsDataFragmentTitle != null) {
                properties.put(MasData.TITLE, sensorsDataFragmentTitle.title());
            }

            if (fragment instanceof MasScreenAutoTracker) {
                MasScreenAutoTracker screenAutoTracker = (MasScreenAutoTracker) fragment;

                String screenUrl = screenAutoTracker.getScreenUrl();
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    mergeJSONObject(otherProperties, properties);
                }

                //MasData.getInstance().trackViewScreen(screenUrl, properties);
            } else {
                MasDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = fragment.getClass().getAnnotation(MasDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    String screenUrl = autoTrackAppViewScreenUrl.url();
                    if (TextUtils.isEmpty(screenUrl)) {
                        screenUrl = fragmentName;
                    }
                    // MasData.getInstance().tr(screenUrl, properties);
                } else {
                    // MasData.getInstance().track("$AppViewScreen", properties);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param object
     */
    public static void trackFragmentResume(Object object) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }

        if (!(object instanceof android.support.v4.app.Fragment)) {
            return;
        }

        android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) object;
        android.support.v4.app.Fragment parentFragment = fragment.getParentFragment();
        if (parentFragment == null) {
            if (!fragment.isHidden() && fragment.getUserVisibleHint()) {
                trackFragmentAppViewScreen(fragment);
            }
        } else {
            if (!fragment.isHidden() && fragment.getUserVisibleHint() && !parentFragment.isHidden() && parentFragment.getUserVisibleHint()) {
                trackFragmentAppViewScreen(fragment);
            }
        }
    }

    /**
     * @param object
     * @param isVisibleToUser
     */
    public static void trackFragmentSetUserVisibleHint(Object object, boolean isVisibleToUser) {

        if (!(object instanceof android.support.v4.app.Fragment)) {
            return;
        }

        android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) object;

        android.support.v4.app.Fragment parentFragment = fragment.getParentFragment();
        if (parentFragment == null) {
            if (isVisibleToUser) {
                if (fragment.isResumed()) {
                    if (!fragment.isHidden()) {
                        trackFragmentAppViewScreen(fragment);
                    }
                }
            }
        } else {
            if (isVisibleToUser && parentFragment.getUserVisibleHint()) {
                if (fragment.isResumed() && parentFragment.isResumed()) {
                    if (!fragment.isHidden() && !parentFragment.isHidden()) {
                        trackFragmentAppViewScreen(fragment);
                    }
                }
            }
        }
    }

    /**
     * fragment隐藏显示
     *
     * @param object
     * @param hidden
     */
    public static void trackOnHiddenChanged(Object object, boolean hidden) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }

        if (!(object instanceof android.support.v4.app.Fragment)) {
            return;
        }

        android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) object;
        android.support.v4.app.Fragment parentFragment = fragment.getParentFragment();
        if (parentFragment == null) {
            if (!hidden) {
                if (fragment.isResumed()) {
                    if (fragment.getUserVisibleHint()) {
                        trackFragmentAppViewScreen(fragment);
                    }
                }
            }
        } else {
            if (!hidden && !parentFragment.isHidden()) {
                if (fragment.isResumed() && parentFragment.isResumed()) {
                    if (fragment.getUserVisibleHint() && parentFragment.getUserVisibleHint()) {
                        trackFragmentAppViewScreen(fragment);
                    }
                }
            }
        }
    }


    /**
     * 判断 View 是否被忽略
     *
     * @param view View
     * @return 是否被忽略
     */
    public static boolean isViewIgnored(View view) {
        try {
            //基本校验
            if (view == null) {
                return true;
            }


            //View 被忽略
            if ("1".equals(view.getTag(R.id.mas_tag_view_ignored))) {
                return true;
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }


    public static void getFragmentNameFromView(View view, JSONObject properties) {
        try {
            if (view != null) {
                String fragmentName = (String) view.getTag(R.id.mas_tag_view_fragment_name);
                String fragmentName2 = (String) view.getTag(R.id.mas_tag_view_fragment_name2);
                if (!TextUtils.isEmpty(fragmentName2)) {
                    fragmentName = fragmentName2;
                }
                if (!TextUtils.isEmpty(fragmentName)) {
                    String screenName = properties.optString(MasData.SCREEN_NAME);
                    if (!TextUtils.isEmpty(fragmentName)) {
                        properties.put(MasData.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", screenName, fragmentName));
                    } else {
                        properties.put(MasData.SCREEN_NAME, fragmentName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String traverseView(StringBuilder stringBuilder, ViewGroup root) {
        try {
            if (root == null) {
                return stringBuilder.toString();
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);

                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }

                if (child instanceof ViewGroup) {
                    traverseView(stringBuilder, (ViewGroup) child);
                } else {
                    if (isViewIgnored(child)) {
                        continue;
                    }

                    CharSequence viewText = null;
                    if (child instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) child;
                        viewText = checkBox.getText();
                    } else if (child instanceof SwitchCompat) {
                        SwitchCompat switchCompat = (SwitchCompat) child;
                        viewText = switchCompat.getTextOn();
                    } else if (child instanceof RadioButton) {
                        RadioButton radioButton = (RadioButton) child;
                        viewText = radioButton.getText();
                    } else if (child instanceof ToggleButton) {
                        ToggleButton toggleButton = (ToggleButton) child;
                        boolean isChecked = toggleButton.isChecked();
                        if (isChecked) {
                            viewText = toggleButton.getTextOn();
                        } else {
                            viewText = toggleButton.getTextOff();
                        }
                    } else if (child instanceof Button) {
                        Button button = (Button) child;
                        viewText = button.getText();
                    } else if (child instanceof CheckedTextView) {
                        CheckedTextView textView = (CheckedTextView) child;
                        viewText = textView.getText();
                    } else if (child instanceof TextView) {
                        TextView textView = (TextView) child;
                        viewText = textView.getText();
                    } else if (child instanceof ImageView) {
                        ImageView imageView = (ImageView) child;
                        if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                            viewText = imageView.getContentDescription().toString();
                        }
                    }

                    if (!TextUtils.isEmpty(viewText)) {
                        stringBuilder.append(viewText.toString());
                        stringBuilder.append("-");
                    }
                }
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return stringBuilder.toString();
        }
    }

    /**
     * ExpandableListView onClick事件
     *
     * @param expandableListView
     * @param view
     * @param groupPosition
     */
    public static void trackExpandableListViewOnGroupClick(ExpandableListView expandableListView, View view, int groupPosition) {

        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {
            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }
            //将 Context 转成 Activity
            Activity activity = null;
            if (context instanceof Activity) {
                activity = (Activity) context;
            }

            // View 被忽略
            if (isViewIgnored(expandableListView)) {
                return;
            }

            JSONObject properties = new JSONObject();

            // $screen_name & $title
            if (activity != null) {
                properties.put(MasData.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(MasData.TITLE, activityTitle);
                }
            }

            // ViewId
            String idString = getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(MasData.ELEMENT_ID, idString);
            }

            properties.put(MasData.ELEMENT_POSITION, String.format(Locale.CHINA, "%d", groupPosition));

            properties.put(MasData.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(MasData.ELEMENT_CONTENT, viewText);
            }

            //screenName
            getFragmentNameFromView(expandableListView, properties);

            // 获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.mas_tag_view_properties);
            if (p != null) {
                mergeJSONObject(p, properties);
            }

            // 扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof MasExpandableListViewItemTrackProperties) {
                    try {
                        MasExpandableListViewItemTrackProperties trackProperties = (MasExpandableListViewItemTrackProperties) listAdapter;
                        JSONObject jsonObject = trackProperties.getGroupItemTrackProperties(groupPosition);
                        if (jsonObject != null) {
                            mergeJSONObject(jsonObject, properties);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * ExpandableListView子view
     *
     * @param expandableListView
     * @param view
     * @param groupPosition
     * @param childPosition
     */
    public static void trackExpandableListViewOnChildClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition, int childPosition) {

        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = getActivityFromContext(context, expandableListView);


            //View 被忽略
            if (isViewIgnored(expandableListView)) {
                return;
            }

            //View 被忽略
            if (isViewIgnored(view)) {
                return;
            }

            //获取 View 自定义属性
            JSONObject properties = (JSONObject) view.getTag(R.id.mas_tag_view_properties);

            if (properties == null) {
                properties = new JSONObject();
            }

            properties.put(MasData.ELEMENT_POSITION, String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));

            //扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof MasExpandableListViewItemTrackProperties) {
                    MasExpandableListViewItemTrackProperties trackProperties = (MasExpandableListViewItemTrackProperties) listAdapter;
                    JSONObject jsonObject = trackProperties.getChildItemTrackProperties(groupPosition, childPosition);
                    if (jsonObject != null) {
                        mergeJSONObject(jsonObject, properties);
                    }
                }
            }

            //$screen_name & $title
            if (activity != null) {
                properties.put(MasData.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(MasData.TITLE, activityTitle);
                }
            }

            //ViewId
            String idString = getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(MasData.ELEMENT_ID, idString);
            }

            properties.put(MasData.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(MasData.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            getFragmentNameFromView(expandableListView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.mas_tag_view_properties);
            if (p != null) {
                mergeJSONObject(p, properties);
            }
            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackTabHost(String tabName) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {
            JSONObject properties = new JSONObject();
            properties.put(MasData.ELEMENT_CONTENT, tabName);
            properties.put(MasData.ELEMENT_TYPE, "TabHost");

            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackTabLayoutSelected(Object object, Object tab) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {
            if (isDeBounceTrack(tab)) {
                return;
            }

            Context context = null;
            if (object instanceof Context) {
                context = (Context) object;
            }

            //将 Context 转成 Activity
            Activity activity = getActivityFromContext(context, null);


            JSONObject properties = new JSONObject();

            //$screen_name & $title
            if (activity != null) {
                properties.put(MasData.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(MasData.TITLE, activityTitle);
                }
            }

            Class<?> tabClass = Class.forName("android.support.design.widget.TabLayout$Tab");
            if (tabClass != null) {
                Method method = tabClass.getMethod("getText");
                if (method != null) {
                    Object text = method.invoke(tab);

                    //Content
                    if (text != null) {
                        properties.put(MasData.ELEMENT_CONTENT, text);
                    }
                }
            }

            //Type
            properties.put(MasData.ELEMENT_TYPE, "TabLayout");
            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackMenuItem(Object object, MenuItem menuItem) {

        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {

            if (isDeBounceTrack(menuItem)) {
                return;
            }

            Context context = null;
            if (object instanceof Context) {
                context = (Context) object;
            }

            //将 Context 转成 Activity
            Activity activity = getActivityFromContext(context, null);


            //获取View ID
            String idString = null;
            try {
                if (context != null) {
                    idString = context.getResources().getResourceEntryName(menuItem.getItemId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            JSONObject properties = new JSONObject();

            //$screen_name & $title
            if (activity != null) {
                properties.put(MasData.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(MasData.TITLE, activityTitle);
                }
            }

            //ViewID
            if (!TextUtils.isEmpty(idString)) {
                properties.put(MasData.ELEMENT_ID, idString);
            }

            //Content
            if (!TextUtils.isEmpty(menuItem.getTitle())) {
                properties.put(MasData.ELEMENT_CONTENT, menuItem.getTitle());
            }

            //Type
            properties.put(MasData.ELEMENT_TYPE, "MenuItem");

            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * "$event_duration":"20",       //类型：数值,浏览时长，毫秒
     * "$screen_name":"Mainactivy",             //类型：页面名称,Activity 的标题（仅 Android 端有）
     * "$title":"com.zmsoft.Mainactivy.Gradle", //类型：页面标题,Activity 的包名.类名（仅 Android 端有）
     * "$element_id":"xxxx",                          //类型：字符串   元素ID（控件的ID）
     * "$element_type":"Button",            //类型：字符串   元素类型(控件的类型)
     * "$element_content":"xxxx",                          //类型：字符串   元素内容（控件的内容）
     * "$element_position":"120*384",            //类型：字符串   元素位置(元素被点击时所处的位置)
     */
    public static void trackViewOnClickEnd() {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }

        try {
            if (Clickproperties == null || eventName == null) {
                return;
            }
            long duration = System.currentTimeMillis() - startTime;
            Clickproperties.put("$event_duration", duration);
            Clickproperties.put("start_timestamp", startTime);
            MasData.getInstance().trackEvent(eventName, Clickproperties);
            eventName = null;
            Clickproperties = null;
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public static void trackRadioGroup(RadioGroup view, int checkedId) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = getActivityFromContext(context, view);

            //View 被忽略
            if (isViewIgnored(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

            //ViewId
            String idString = getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(MasData.ELEMENT_ID, idString);
            }

            //$screen_name & $title
            if (activity != null) {
                properties.put(MasData.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(MasData.TITLE, activityTitle);
                }
            }

            properties.put(MasData.ELEMENT_TYPE, "RadioButton");

            //获取变更后的选中项的ID
            int checkedRadioButtonId = view.getCheckedRadioButtonId();
            if (activity != null) {
                try {
                    RadioButton radioButton = (RadioButton) activity.findViewById(checkedRadioButtonId);
                    if (radioButton != null) {
                        if (!TextUtils.isEmpty(radioButton.getText())) {
                            String viewText = radioButton.getText().toString();
                            if (!TextUtils.isEmpty(viewText)) {
                                properties.put(MasData.ELEMENT_CONTENT, viewText);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //fragmentName
            getFragmentNameFromView(view, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.mas_tag_view_properties);
            if (p != null) {
                mergeJSONObject(p, properties);
            }
            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackDialog(DialogInterface dialogInterface, int whichButton) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {

            //获取所在的Context
            Dialog dialog = null;
            if (dialogInterface instanceof Dialog) {
                dialog = (Dialog) dialogInterface;
            }

            if (dialog == null) {
                return;
            }

            if (isDeBounceTrack(dialog)) {
                return;
            }

            Context context = dialog.getContext();

            //将Context转成Activity
            Activity activity = getActivityFromContext(context, null);

            if (activity == null) {
                activity = dialog.getOwnerActivity();
            }

            JSONObject properties = new JSONObject();

            try {
                if (dialog.getWindow() != null) {
                    String idString = (String) dialog.getWindow().getDecorView().getTag(R.id.mas_tag_view_id);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(MasData.ELEMENT_ID, idString);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //$screen_name & $title
            if (activity != null) {
                properties.put(MasData.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(MasData.TITLE, activityTitle);
                }
            }

            properties.put(MasData.ELEMENT_TYPE, "Dialog");

            if (dialog instanceof android.app.AlertDialog) {
                android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                Button button = alertDialog.getButton(whichButton);
                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(MasData.ELEMENT_CONTENT, button.getText());
                    }
                } else {
                    ListView listView = alertDialog.getListView();
                    if (listView != null) {
                        ListAdapter listAdapter = listView.getAdapter();
                        Object object = listAdapter.getItem(whichButton);
                        if (object != null) {
                            if (object instanceof String) {
                                properties.put(MasData.ELEMENT_CONTENT, object);
                            }
                        }
                    }
                }

            } else if (dialog instanceof android.support.v7.app.AlertDialog) {
                android.support.v7.app.AlertDialog alertDialog = (android.support.v7.app.AlertDialog) dialog;
                Button button = alertDialog.getButton(whichButton);
                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(MasData.ELEMENT_CONTENT, button.getText());
                    }
                } else {
                    ListView listView = alertDialog.getListView();
                    if (listView != null) {
                        ListAdapter listAdapter = listView.getAdapter();
                        Object object = listAdapter.getItem(whichButton);
                        if (object != null) {
                            if (object instanceof String) {
                                properties.put(MasData.ELEMENT_CONTENT, object);
                            }
                        }
                    }
                }
            }
            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackListView(AdapterView<?> adapterView, View view, int position) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {
            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = getActivityFromContext(context, view);


            JSONObject properties = new JSONObject();

            if (adapterView instanceof ListView) {
                properties.put(MasData.ELEMENT_TYPE, "ListView");

            } else if (adapterView instanceof GridView) {
                properties.put(MasData.ELEMENT_TYPE, "GridView");
            }

            //ViewId
            String idString = getViewId(adapterView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(MasData.ELEMENT_ID, idString);
            }

            //扩展属性
            Adapter adapter = adapterView.getAdapter();
            if (adapter != null && adapter instanceof MasAdapterViewItemTrackProperties) {
                try {
                    MasAdapterViewItemTrackProperties objectProperties = (MasAdapterViewItemTrackProperties) adapter;
                    JSONObject jsonObject = objectProperties.getItemTrackProperties(position);
                    if (jsonObject != null) {
                        mergeJSONObject(jsonObject, properties);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            //Activity 名称和页面标题
            if (activity != null) {
                properties.put(MasData.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(MasData.TITLE, activityTitle);
                }
            }

            //点击的 position
            properties.put(MasData.ELEMENT_POSITION, String.valueOf(position));

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (view instanceof TextView) {
                viewText = ((TextView) view).getText().toString();
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(MasData.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            getFragmentNameFromView(adapterView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.mas_tag_view_properties);
            if (p != null) {
                mergeJSONObject(p, properties);
            }
            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置View属性
     *
     * @param view       要设置的View
     * @param properties 要设置的View的属性
     */
    public static void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }

        view.setTag(R.id.mas_tag_view_properties, properties);
    }

    public static void trackDrawerOpened(View view) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Open");
            setViewProperties(view, jsonObject);
            trackViewOnClick(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackDrawerClosed(View view) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Close");
            setViewProperties(view, jsonObject);
            trackViewOnClick(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void trackViewOnClick(View view) {
        if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
            return;
        }
        try {

            if (!Monitor.isInstalled(MonitorData.TYPE_APP_CLICK)) {
                return;
            }
            //获取所在的 Context
            Context context = view.getContext();

            //将 Context 转成 Activity
            Activity activity = getActivityFromContext(context, view);

            //View 被忽略
            if (isViewIgnored(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

            //ViewId
            String idString = getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(MasData.ELEMENT_ID, idString);
            }

            //$screen_name & $title
            if (activity != null) {
                properties.put(MasData.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(MasData.TITLE, activityTitle);
                }
            }

            String viewType = view.getClass().getCanonicalName();

            Class<?> switchCompatClass = null;
            try {
                switchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
            } catch (Exception e) {
                //ignored
            }

            CharSequence viewText = null;
            if (view instanceof CheckBox) { // CheckBox
                viewType = "CheckBox";
                CheckBox checkBox = (CheckBox) view;
                viewText = checkBox.getText();
            } else if (view instanceof ViewPager) {
                viewType = "ViewPager";
                try {
                    ViewPager viewPager = (ViewPager) view;
                    viewText = viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (switchCompatClass != null && switchCompatClass.isInstance(view)) {
                viewType = "SwitchCompat";
                CompoundButton switchCompat = (CompoundButton) view;
                Method method;
                if (switchCompat.isChecked()) {
                    method = view.getClass().getMethod("getTextOn");
                } else {
                    method = view.getClass().getMethod("getTextOff");
                }
                viewText = (String) method.invoke(view);
            } else if (view instanceof RadioButton) { // RadioButton
                viewType = "RadioButton";
                RadioButton radioButton = (RadioButton) view;
                viewText = radioButton.getText();
            } else if (view instanceof ToggleButton) { // ToggleButton
                viewType = "ToggleButton";
                ToggleButton toggleButton = (ToggleButton) view;
                boolean isChecked = toggleButton.isChecked();
                if (isChecked) {
                    viewText = toggleButton.getTextOn();
                } else {
                    viewText = toggleButton.getTextOff();
                }
            } else if (view instanceof Button) { // Button
                viewType = "Button";
                Button button = (Button) view;
                viewText = button.getText();
            } else if (view instanceof CheckedTextView) { // CheckedTextView
                viewType = "CheckedTextView";
                CheckedTextView textView = (CheckedTextView) view;
                viewText = textView.getText();
            } else if (view instanceof TextView) { // TextView
                viewType = "TextView";
                TextView textView = (TextView) view;
                viewText = textView.getText();
            } else if (view instanceof ImageView) { // ImageView
                viewType = "ImageView";
                ImageView imageView = (ImageView) view;
                if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                    viewText = imageView.getContentDescription().toString();
                }
            } else if (view instanceof RatingBar) {
                viewType = "RatingBar";
                RatingBar ratingBar = (RatingBar) view;
                viewText = String.valueOf(ratingBar.getRating());
            } else if (view instanceof SeekBar) {
                viewType = "SeekBar";
                SeekBar seekBar = (SeekBar) view;
                viewText = String.valueOf(seekBar.getProgress());
            } else if (view instanceof Spinner) {
                viewType = "Spinner";
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //view的屏幕位置
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            properties.put("$element_position", "" + location[0] + "*" + location[1]);
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(MasData.ELEMENT_CONTENT, viewText.toString());
            }

            //$element_type
            properties.put(MasData.ELEMENT_TYPE, viewType);

            //fragmentName
            getFragmentNameFromView(view, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.mas_tag_view_properties);
            if (p != null) {
                mergeJSONObject(p, properties);
            }
            eventName = MasData.APP_CLICK_EVENT_NAME;
            Clickproperties = properties;
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param view
     * @return
     */
    public static String getViewId(View view) {
        String idString = null;
        try {
            idString = (String) view.getTag(R.id.mas_tag_view_id);
            if (TextUtils.isEmpty(idString)) {
                if (view.getId() != View.NO_ID) {
                    idString = view.getContext().getResources().getResourceEntryName(view.getId());
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return idString;
    }

}
