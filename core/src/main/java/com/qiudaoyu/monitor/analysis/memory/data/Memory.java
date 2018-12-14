package com.qiudaoyu.monitor.analysis.memory.data;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.support.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static android.os.Build.VERSION_CODES.M;


/**
 * https://mp.weixin.qq.com/s/KtGfi5th-4YHOZsEmTOsjg
 *
 * <p>
 * API23 6.0 版本及以上运行时获取内存的方法
 * ActivityManager 的 getProcessMemoryInfo，获取进程的 Debug.MemoryInfo 数据
 * 这个接口在低端机型中可能耗时较久，不能在主线程中调用，且监控调用耗时，在耗时过大的机型上，屏蔽内存监控模块
 * 调用 Debug.MemoryInfo 的 getMemoryStats 方法
 * <p>
 * <p>
 * <p>
 * API23 6.0 以下的运行时获取内存的方法
 * Runttime
 * <p>
 * adb shell procrank
 * Vss>>Rss>>Pss>>Uss
 * VSS - Virtual Set Size 虚拟耗用内存（包含共享库占用的内存）
 *  RSS - Resident Set Size 实际使用物理内存（包含共享库占用的内存）
 *  PSS - Proportional Set Size 实际使用的物理内存（比例分配共享库占用的内存）
 *  USS - Unique Set Size 进程独自占用的物理内存（不包含共享库占用的内存）
 * <p>
 * dumpsys meminfo原理
 * adb shell dumpsys meminfo <package_name>/<pid>
 * https://blog.csdn.net/msf568834002/article/details/78881341
 * https://juejin.im/post/5a3cc4416fb9a0450d11453e
 * <p>
 * <p>
 * <p>
 * javaheap   已用，可用，总大小
 * <p>
 * nativeheap 总大小
 * <p>
 * 进程的Pss大小
 */
public class Memory {

    private static AtomicLong sTotalMem = new AtomicLong(0L);


    private static volatile boolean isCostHigh;

    /**
     * 直接获取类适于profiler中的数据
     * 这个接口在低端机型中可能耗时较久，不能在主线程中调用，在耗时过大的机型上，需要不执行内存监控模块
     *
     * @param context
     * @return
     */
    @RequiresApi(api = M)
    public static Map<String, String> getProcessMemoryInfo(Context context) {
        final int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        Debug.MemoryInfo memoryInfo = am.getProcessMemoryInfo(new int[]{pid})[0];
        Map<String, String> stats = memoryInfo.getMemoryStats();
        return stats;
    }


    /**
     * https://i.stack.imgur.com/GjuwM.png
     * https://www.jianshu.com/p/1ee91ff0204f
     * <p>
     * 获取应用堆内存信息
     * 耗时忽略不计
     *
     * @return 堆内存KB
     */
    public static HeapInfo getAppHeapInfo() {
        Runtime runtime = Runtime.getRuntime();
        HeapInfo heapInfo = new HeapInfo();
        heapInfo.freeMemKb = runtime.freeMemory() / 1024;
        heapInfo.maxMemKb = Runtime.getRuntime().maxMemory() / 1024;
        heapInfo.allocatedKb = (Runtime.getRuntime().totalMemory() - runtime.freeMemory()) / 1024;
        return heapInfo;
    }


    /**
     * native分配的内存大小
     *
     * @return
     */
    public static long getAppNatvieInfo() {
        return Debug.getNativeHeapAllocatedSize() / 1024;
    }


    /**
     * 获取系统的内存数据
     *
     * @param context
     * @return
     */
    public static RamInfo getSysRamInfo(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        final RamInfo ramMemoryInfo = new RamInfo();
        ramMemoryInfo.availMemKb = mi.availMem / 1024;
        ramMemoryInfo.isLowMemory = mi.lowMemory;
        ramMemoryInfo.lowMemThresholdKb = mi.threshold / 1024;
        ramMemoryInfo.totalMemKb = getSysRamTotalMem(am);
        return ramMemoryInfo;
    }

    /**
     * 获取系统的总的ram大小
     *
     * @param activityManager
     * @return
     */
    private static long getSysRamTotalMem(ActivityManager activityManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(mi);
            return mi.totalMem / 1024;
        } else if (sTotalMem.get() > 0L) {//如果已经从文件获取过值，则不需要再次获取
            return sTotalMem.get();
        } else {
            final long tm = getSysRamTotalMemByFile();
            sTotalMem.set(tm);
            return tm;
        }
    }

    /**
     * 获取手机的RAM容量，其实和activityManager.getMemoryInfo(mi).totalMem效果一样，也就是说，
     * 在API16以上使用系统API获取，低版本采用这个文件读取方式
     *
     * @return 容量KB
     */
    private static long getSysRamTotalMemByFile() {
        final String dir = "/proc/meminfo";
        try {
            FileReader fr = new FileReader(dir);
            BufferedReader br = new BufferedReader(fr, 2048);
            String memoryLine = br.readLine();
            String subMemoryLine = memoryLine.substring(memoryLine
                    .indexOf("MemTotal:"));
            br.close();
            long totalMemorySize = Integer.parseInt(subMemoryLine.replaceAll(
                    "\\D+", ""));
            return totalMemorySize;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0L;
    }


    /**
     * 获取meminfo内容
     *
     * @param context
     * @return
     */
    public static MemInfo getAppMemInfo(Context context) {
        MemInfo memInfo = new MemInfo();
        memInfo.time = System.currentTimeMillis();
        //进程总内存
        if (Build.VERSION.SDK_INT > M && !isCostHigh) {
            long time = System.currentTimeMillis();
            final int pid = android.os.Process.myPid();
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            Debug.MemoryInfo memoryInfo = am.getProcessMemoryInfo(new int[]{pid})[0];
            memInfo.proMemKb = memoryInfo.getTotalPss();
            if ((System.currentTimeMillis() - time) > 200) {
                isCostHigh = true;
            }
        }


        Runtime runtime = Runtime.getRuntime();
        //堆数据
        memInfo.freeMemKb = runtime.freeMemory() / 1024;
        memInfo.maxMemKb = Runtime.getRuntime().maxMemory() / 1024;
        memInfo.allocatedKb = (Runtime.getRuntime().totalMemory() - runtime.freeMemory()) / 1024;
        //native内存
        memInfo.nativeAllocatedKb = Debug.getNativeHeapAllocatedSize() / 1024;

        if (memInfo.proMemKb == 0) {
            memInfo.proMemKb = memInfo.freeMemKb + memInfo.allocatedKb + memInfo.nativeAllocatedKb;
        }

        //系统内存状态
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        memInfo.availMemKb = mi.availMem / 1024;
        memInfo.isLowMemory = mi.lowMemory;
        memInfo.lowMemThresholdKb = mi.threshold / 1024;
        memInfo.totalMemKb = getSysRamTotalMem(am);


        return memInfo;
    }
}
