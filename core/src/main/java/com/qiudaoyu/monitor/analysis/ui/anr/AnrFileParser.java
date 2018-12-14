package com.qiudaoyu.monitor.analysis.ui.anr;

import android.content.Context;
import android.text.TextUtils;

import com.tencent.mmkv.MMKV;
import com.qiudaoyu.monitor.utils.AppUtils;
import com.qiudaoyu.monitor.utils.IoUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * anr文件解析类
 * <p>
 * ----- pid 31988 at 2018-12-11 11:03:47 -----
 * Cmd line: com.test.test
 * <p>
 * <p>
 * ----- end 31988 -----
 * <p>
 * anr数据一般保持最后一次数据
 * <p>
 * 但是 Huawei 最新的anr是写在最后的
 */
public class AnrFileParser {
    public static final String TAG = "AnrFileParser";
    public static final String KEY_TIME = "time";
    public static final String KEY_PID = "pid";

    private static Pattern startPattern = Pattern.compile("-{5}\\spid\\s\\d+\\sat\\s\\d+-\\d+-\\d+\\s\\d{2}:\\d{2}:\\d{2}\\s-{5}"); //第一行

    private static Pattern endPattern = Pattern.compile("-{5}\\send\\s\\d+\\s-{5}"); //最后一行

    private static Pattern cmdLinePattern = Pattern.compile("Cmd\\sline:\\s(\\S+)"); //第二行 cmd进程名称

    private static SimpleDateFormat localSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    /**
     * 只去最新的anr信息
     *
     * @param context
     * @param path
     * @return
     */
    public static AnrInfo parseFile(Context context, String path) {
        AnrInfo anrInfo = null;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(path));
            String buffer;
            StringBuffer stringBuffer = null;

            boolean isValid = true;
            //按行读取anr文件内容
            while ((buffer = bufferedReader.readLine()) != null) {
                if (TextUtils.isEmpty(buffer)) {
                    continue;
                }
                if (startPattern.matcher(buffer).matches()) {
                    // 需要判断当前的anr信息是否曾经读过，若读过，则跳过
                    String[] sections = buffer.split("\\s");
                    if (sections != null && sections.length > 0) {
                        String pidStr = sections[2].trim();
                        String timeStr = sections[4].trim() + " " + sections[5].trim();
                        long pid = Long.valueOf(pidStr);
                        long ts = localSimpleDateFormat.parse(timeStr).getTime();
                        if (!isUploaded(pid, ts)) {
                            anrInfo = new AnrInfo();
                            stringBuffer = new StringBuffer();
                            anrInfo.setTime(ts);
                            anrInfo.setProId(pid);
                            appendContent(stringBuffer, buffer);
                            isValid = true;
                        } else {
                            isValid = false;
                        }
                    }
                } else if (endPattern.matcher(buffer).matches()) {
                    if (!isValid || anrInfo == null) {
                        continue;
                    }
                    String[] sections = buffer.split("\\s");
                    String pidStr = sections[2].trim();
                    long pid = Long.valueOf(pidStr);
                    appendContent(stringBuffer, buffer);
                    if (pid == anrInfo.getProId()) {
                        anrInfo.setAnrContent(stringBuffer.toString());
                        return anrInfo; //解析成功
                    }
                    return null;
                } else if (cmdLinePattern.matcher(buffer).matches()) {
                    if (!isValid || anrInfo == null) {
                        continue;
                    }
                    String proName = getProName(buffer);
                    if (anrInfo != null) {
                        anrInfo.setProName(proName);
                    }
                    //进程是否有效
                    if (!proName.contains(AppUtils.getPkgName(context))) {
                        //这一行不是我们想要的数据，无效anr文件
                        isValid = false;
                        continue;
                    }
                    appendContent(stringBuffer, buffer);
                } else {
                    if (!isValid || anrInfo == null) {
                        continue;
                    }
                    appendContent(stringBuffer, buffer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IoUtil.closeSilently(bufferedReader);
        }
        return null;
    }

    private static String getProName(String buffer) {
        String proName = "";
        int index = buffer.indexOf(":");
        index++;
        if (index >= 0 && index < buffer.length()) {
            proName = buffer.substring(index).trim();
        }
        return proName;
    }

    private static void appendContent(StringBuffer stringBuffer, String content) {
        if (stringBuffer != null) {
            stringBuffer.append(content).append("\n");
        }
    }

    /**
     * 判断是否已经上传过
     */
    private static boolean isUploaded(long pid, long ts) {
        Set<String> oldPidTimes = MMKV.mmkvWithID("monitor").getStringSet(KEY_PID, null);

        if (oldPidTimes == null) {
            return false;
        }

        if (oldPidTimes.contains(String.valueOf(pid) + String.valueOf(ts))) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否已经上传过
     */
    public static void setUploadedKey(long pid, long ts) {
        Set<String> oldPidTimes = MMKV.mmkvWithID("monitor").getStringSet(KEY_PID, null);
        if (oldPidTimes == null) {
            oldPidTimes = new HashSet<>();
        }
        oldPidTimes.add(String.valueOf(pid) + String.valueOf(ts));
        MMKV.mmkvWithID("monitor").putStringSet(KEY_PID, oldPidTimes);
    }
}
