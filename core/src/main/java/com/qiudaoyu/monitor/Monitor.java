package com.qiudaoyu.monitor;

import android.content.Context;
import android.util.SparseArray;

import com.qiudaoyu.monitor.analysis.MasData;
import com.qiudaoyu.monitor.analysis.battery.BatteryMonitor;
import com.qiudaoyu.monitor.analysis.cpu.CpuMonitor;
import com.qiudaoyu.monitor.analysis.crash.CrashMonitor;
import com.qiudaoyu.monitor.analysis.memory.MemMonitor;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.analysis.network.NetMonitor;
import com.qiudaoyu.monitor.analysis.ui.UIMonitor;
import com.qiudaoyu.monitor.log.dynamiclog.config.ConfigManager;
import com.qiudaoyu.monitor.upload.UploadServiceInterface;
import com.tencent.mmkv.MMKV;
import com.qiudaoyu.monitor.analysis.MasData;
import com.qiudaoyu.monitor.analysis.battery.BatteryMonitor;
import com.qiudaoyu.monitor.analysis.cpu.CpuMonitor;
import com.qiudaoyu.monitor.analysis.crash.CrashMonitor;
import com.qiudaoyu.monitor.analysis.memory.MemMonitor;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.metric.MetricMonitor;
import com.qiudaoyu.monitor.analysis.network.NetMonitor;
import com.qiudaoyu.monitor.analysis.ui.UIMonitor;
import com.qiudaoyu.monitor.log.MLog;
import com.qiudaoyu.monitor.log.dynamiclog.config.ConfigManager;
import com.qiudaoyu.monitor.log.mlog.MAopHandler;
import com.qiudaoyu.monitor.upload.UploadServiceInterface;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * MAOP
 * 中存在code属性的方法
 * 1.下列方法无法监测
 * 接口的方法
 * abstract方法
 * 父类中没有的重写的方法
 * native方法
 * 2.动态代理的生成的类的方法无法直接监测
 * 3.在编译过程使用的是Stub的类无法监测
 * android framework中的类
 * 4.剔除编译器自动生成的
 * access$
 * ,<init>
 * ,<clinit>
 * ,invokeByMethodIndex方法
 * 5.剔除Monitor包下的类
 * <p>
 *
 * <p>
 * 内部类的编译
 * 1.内部类会编译为顶级类
 * 内部类编译为名字的Outer$Inner
 * 内部匿名类编译为名字的Outer$X(X为正整数)
 * 内部类嵌套
 * Outer$Inner$1
 * Outer$1$1
 * 2.新增方法
 * 外部类和内部类相互访问private字段时会生成access$方法
 * <p>
 *
 * Monitor产生提供监控框架，产生各种指标数据
 * MasData用来加工原始数据，再通过UploadService发出
 */
public class Monitor {

    private static final int LOG_TYPE_MANUAL = 1;

    private static final int LOG_TYPE_DYNAMIC = 2;

    private static final int LOG_TYPE_MANUAL_AND_DYNAMIC = 3;

    private static final String TAG = "Monitor";

    public static MAopHandler handler;

    public static boolean isDebug;
    private volatile static UploadServiceInterface uploadService;
    private static ThreadLocal<Boolean> isIndexInvoke = new ThreadLocal<>();
    private static volatile boolean isActive;
    private static Context context;
    private static volatile SparseArray<Integer> monitorTypes;

    private static MetricListener listener;

    private static List<MetricMonitor> metricMonitors;

    public static Context getContext() {
        return context;
    }

    private static void initialize(Context context, int[] types) {

        isActive = true;
        Monitor.context = context;
        monitorTypes = new SparseArray<>();


        if (types == null || types.length == 0) {
            monitorTypes.clear();
        } else {
            monitorTypes.clear();
            for (int i = 0; i < types.length; i++) {
                monitorTypes.put(types[i], 1);
            }
        }

        listener = new MetricListener() {
            @Override
            public void metric(int type, Object data) {
                MasData.getInstance().metricInfo(type, data);
            }
        };
        metricMonitors = new ArrayList<>();
        metricMonitors.add(new MemMonitor(listener));
        metricMonitors.add(new UIMonitor(listener));
        metricMonitors.add(new CpuMonitor(listener));
        metricMonitors.add(new NetMonitor(listener));
        metricMonitors.add(new BatteryMonitor(listener));
        metricMonitors.add(new CrashMonitor(listener));
    }

    public static void start() {
        if (metricMonitors == null) {
            return;
        }
        for (MetricMonitor metric : metricMonitors) {
            if (metric != null) {
                metric.start(context);
            }
        }
    }

    /**
     * @param isForground
     */
    public static void setForground(boolean isForground) {

        if (uploadService != null) {
            uploadService.setForground(isForground);
        }

        if (metricMonitors == null) {
            return;
        }

        for (MetricMonitor metric : metricMonitors) {
            if (metric != null) {
                metric.setForground(isForground);
            }
        }
    }


    /**
     *
     */
    public static void shutDown() {
        isActive = false;
        if (uploadService != null) {
            uploadService.shutDown();
        }
        if (metricMonitors != null) {
            for (MetricMonitor metric : metricMonitors) {
                if (metric != null) {
                    metric.stop(context);
                }
            }
            metricMonitors.clear();
        }
    }


    /**
     * 如何确定调用的的方法：
     * 使用onewayhash方法计算clazzName+methodName+desc计算3个hash值，碰撞率几乎为0
     * https://blog.csdn.net/v_JULY_v/article/details/6256463
     *
     * @param nHash
     * @param nHashA
     * @param nHashB
     * @return
     */
    public static int isEnable(int nHash, int nHashA, int nHashB, int logType) {
        if (!isActive) {
            return 0;
        }
        //如果是反射调用过来，执行真正的代码
        if (isIndexInvoke.get() != null && isIndexInvoke.get().booleanValue()) {
            isIndexInvoke.set(false);
            return 0;
        }

        //如果有手动log的情况
        if ((logType & LOG_TYPE_MANUAL) > 0) {
            return 1;
        }

        return 0;
    }


    /**
     * 只在invokeByMethodIndex中调用
     * 设置是反射调动原来的方法
     */
    public static void invoke() {
        isIndexInvoke.set(true);
    }

    /**
     * Lua 执行是否有足够的性能
     * <p>
     * LuaState
     *
     * @param instanceOrClass 当前的类或者实例
     * @param args            方法参数
     */

    public static void before(int nHash, int nHashA, int nHashB, String desc, Object instanceOrClass, Object[] args, int logtype, Object params) {
        if (!isActive) {
            return;
        }
        if ((logtype & LOG_TYPE_MANUAL) > 0) {
            if (handler != null) {
                try {
                    handler.before((String[]) params, instanceOrClass, args);
                } catch (Exception e) {
                    throw e;
                }
            }

        }

        //如果这个方法只有mlog
        if ((logtype & LOG_TYPE_DYNAMIC) == 0) {
            return;
        }


    }

    /**
     * 异常执行时代码
     *
     * @param e 执行异常
     */
    public static void exception(Exception e, int nHash, int nHashA, int nHashB, String desc, Object instanceOrClass, Object[] args, int logtype, Object params) {
        if (!isActive) {
            return;
        }
        if ((logtype & LOG_TYPE_MANUAL) > 0) {
            if (handler != null) {
                try {
                    handler.exception(e, (String[]) params, instanceOrClass, args);
                } catch (Exception ex) {
                    throw ex;
                }
            }

        }


    }

    /**
     * 0-代表异常退出调用 returnVaule为null
     * 1-代表正常退出调用
     *
     * @param returnVaule 方法返回值
     * @param mode        调用点
     */
    public static void after(Object returnVaule, int mode, int nHash, int nHashA, int nHashB, String desc, Object instanceOrClass, Object[] args, int logtype, Object params) {
        if (!isActive) {
            return;
        }

        if ((logtype & LOG_TYPE_MANUAL) > 0) {
            if (handler != null) {
                try {
                    handler.after(mode, returnVaule, (String[]) params, instanceOrClass, args);
                } catch (Exception e) {
                    throw e;
                }
            }
        }

        if ((logtype & LOG_TYPE_DYNAMIC) == 0) {
            return;
        }
    }

    /**
     * 是否检测某项数据
     *
     * @param monitorType
     * @return
     */
    public static boolean isInstalled(int monitorType) {
        if (monitorTypes != null && monitorTypes.get(monitorType) != null) {
            return true;
        }
        return false;
    }

    /**
     * 指标日志
     *
     * @param type
     * @param data
     */
    public static void metricData(int type, Object data) {
        if (listener != null) {
            listener.metric(type, data);
        }
    }

    /**
     * 构造器
     */
    public static class Builder {
        private boolean isDebug;
        private boolean level = true;
        private MLog.LogInterface logInterface;
        private String prefix;
        private Context context;
        private UploadServiceInterface uploadServiceInterface;
        private ConfigManager manager;
        private MAopHandler handler;
        private int[] types;

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder isDebugEnalbe(boolean isDebug) {
            this.isDebug = isDebug;
            return this;
        }

        public Builder MLog(boolean level, String prefix, MLog.LogInterface logInterface) {
            this.level = level;
            this.logInterface = logInterface;
            this.prefix = prefix;
            return this;
        }

        public Builder context(Context context) {
            this.context = context;
            return this;
        }

        public Builder uploadService(UploadServiceInterface uploadServiceInterface) {
            this.uploadServiceInterface = uploadServiceInterface;
            return this;
        }

        public Builder configManager(ConfigManager manager) {
            this.manager = manager;
            return this;
        }

        public Builder mAopHandler(MAopHandler handler) {
            this.handler = handler;
            return this;
        }

        public Builder install(int[] types) {
            this.types = types;
            return this;
        }

        public Monitor build() {
            Monitor.isDebug = isDebug;
            Monitor.handler = handler;
            Monitor.uploadService = uploadServiceInterface;
            MLog.initialize(level, prefix, logInterface);
            initialize(context, types);
            MMKV.initialize(context);
            MasData.getInstance().setUploadService(Monitor.uploadService);
            return null;
        }


    }


}
