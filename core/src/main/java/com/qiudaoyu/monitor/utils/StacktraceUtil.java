package com.qiudaoyu.monitor.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
public class StacktraceUtil {
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    /**
     * 原始的堆栈信息转换为字符串类型的堆栈信息
     *
     * @param ts
     * @return
     */
    public static Map<String, String> convertToStackString(Map<Long, StackTraceElement[]> ts) {
        // 筛选之后的堆栈
        Map<Long, StackTraceElement[]> filterMap = new LinkedHashMap<>();
        for (Long key : ts.keySet()) {
            StackTraceElement[] value = ts.get(key);
            if (!filterMap.containsValue(value)) {// 筛选一下是否存在堆栈信息相同的
                filterMap.put(key, value);
            }
        }
        // 转换为字符串
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<Long, StackTraceElement[]> entry : filterMap.entrySet()) {
            result.put(TIME_FORMATTER.format(entry.getKey()), getStackString(entry.getValue()));
        }
        return result;
    }

    private static String getStackString(StackTraceElement[] stackTraceElements) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement traceElement : stackTraceElements) {
            builder.append(String.valueOf(traceElement));
        }
        return builder.toString();
    }

    private static List<String> getStack(List<StackTraceElement> stackTraceElements) {
        List<String> stackList = new ArrayList<>();
        for (StackTraceElement traceElement : stackTraceElements) {
            stackList.add(String.valueOf(traceElement));
        }
        return stackList;
    }

    public static List<String> getStack(StackTraceElement... stackTraceElements) {
        List<String> stackList = new ArrayList<>();
        for (StackTraceElement traceElement : stackTraceElements) {
            stackList.add(String.valueOf(traceElement));
        }
        return stackList;
    }
}
