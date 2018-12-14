package com.qiudaoyu.monitor.analysis.memory.gc;

/**
 * 创建时间: 2018/12/4
 * 类描述:
 * <p>
 * 收集logcat中的GC日志
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class Gc {

    public static final String SHELL_COMMAND = "logcat -v time %s | grep GC";
    //logcat捕捉GC日志
    //Dalvik/Art
    //adb shell logcat com.zmsoft.kds | grep -e GC_ -e AllocSpace


}
