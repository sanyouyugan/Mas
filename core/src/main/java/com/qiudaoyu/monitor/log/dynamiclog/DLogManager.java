package com.qiudaoyu.monitor.log.dynamiclog;

import com.qiudaoyu.monitor.log.dynamiclog.context.MonitorContext;
import com.qiudaoyu.monitor.log.dynamiclog.lua.LuaConstance;
import com.qiudaoyu.monitor.log.dynamiclog.lua.LuaRunException;
import com.qiudaoyu.monitor.log.dynamiclog.lua.LuaUtils;
import com.qiudaoyu.monitor.log.dynamiclog.pool.ObjectPool;
import com.qiudaoyu.monitor.log.dynamiclog.context.MonitorContext;
import com.qiudaoyu.monitor.log.dynamiclog.lua.LuaConstance;
import com.qiudaoyu.monitor.log.dynamiclog.lua.LuaRunException;
import com.qiudaoyu.monitor.log.dynamiclog.lua.LuaUtils;
import com.qiudaoyu.monitor.log.dynamiclog.pool.ObjectPool;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 创建时间: 2018/12/10
 * 类描述:
 * <p>
 * 如何定位采集点
 * 使用onewayhash方法计算clazzName+methodName+desc计算3个hash值，碰撞率几乎为0
 * https://blog.csdn.net/v_JULY_v/article/details/6256463
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class DLogManager {
    public DLogManager() {
        MonitorContext[] arrays = new MonitorContext[128];
        ObjectPool pools = new ObjectPool<>(arrays, new ObjectPool.RecyclableFactory() {
            @Override
            public ObjectPool.RecyclableObject createNew() {
                return new MonitorContext();
            }
        });
    }

    public static int isEnable(int nHash, int nHashA, int nHashB, int logType) {


//        if (!isActive) {
//            return 0;
//        }
//        //如果是反射调用过来，执行真正的代码
//        if (isIndexInvoke.get() != null && isIndexInvoke.get().booleanValue()) {
//            isIndexInvoke.set(false);
//            return 0;
//        }
//
//        //如果有手动log的情况
//        if ((logType & LOG_TYPE_MANUAL) > 0) {
//            return 1;
//        }

        return 0;
    }


    public static void before(int nHash, int nHashA, int nHashB, String desc, Object instanceOrClass, Object[] args, int logtype, Object params) {


//        //存在动态日志
//        MethodConfig config = manager.getConfig(nHash, nHashA, nHashB);
//        if (config == null) {
//            return;
//        }
//        //如果没有初始化，加载当前方法的luastring
//        if (EmptyUtils.isNotEmpty(config.getLuaString())
//                && getMonitorContext().isInited()) {
//
//            LuaState luaState = getMonitorContext().getState();
//
//            //执行每个方法配置的初始化
//            int reslut = luaState.LdoString(config.getLuaString());
//
//            if (reslut == 0) {
//                //执行lua initTop方法
//                try {
//                    callMonitorRootLuaFunc(LuaConstance.INITTOP, getList(instanceOrClass, args));
//                    getMonitorContext().pushTopArg(Boolean.TRUE);
//                } catch (Exception ex) {
//                    //  执行失败，log error
//                    getMonitorContext().pushTopArg(Boolean.FALSE);
//                    MLog.d(MONITOR, "run INITTOP " + "result " + reslut);
//
//                }
//            } else {
//                //  加载失败，log error
//                getMonitorContext().pushTopArg(Boolean.FALSE);
//                MLog.d(MONITOR, "run lua " + config.getLuaString() + "result " + reslut);
//
//            }
//        } else {
//            //  log error
//            getMonitorContext().pushTopArg(Boolean.FALSE);
//            MLog.d(MONITOR, "before not run " + config.getLuaString() + "init " + getMonitorContext().isInited());
//
//        }
//
//
//        if (getMonitorContext().isInited()
//                && getMonitorContext().getTopArg()
//                && config.isBefore()) {
//            //调用before方法
//            try {
//                callMonitorThisMethodLuaFunc(LuaConstance.BEFORE);
//            } catch (Exception ex) {
//                // 执行失败，log error
//                MLog.d(MONITOR, "run BEFORE ", ex);
//            }
//
//        }


    }

    /**
     * 异常执行时代码
     *
     * @param e 执行异常
     */
    public static void exception(Exception e, int nHash, int nHashA, int nHashB, String desc, Object instanceOrClass, Object[] args, int logtype, Object params) {


//        if ((logtype & LOG_TYPE_DYNAMIC) == 0) {
//            return;
//        }
//        MethodConfig config = manager.getConfig(nHash, nHashA, nHashB);
//
//        if (config == null) {
//            return;
//        }
//        if (config.isException()
//                && getMonitorContext().isInited()
//                && getMonitorContext().getTopArg()) {
//            //调用exception方法方法
//            try {
//                callMonitorThisMethodLuaFunc(LuaConstance.EXCEPTION, e);
//            } catch (Exception ex) {
//                //  执行失败，log error
//                MLog.d(MONITOR, "run EXCEPTION ", ex);
//            }
//
//        }


    }

    /**
     * 0-代表异常退出调用 returnVaule为null
     * 1-代表正常退出调用
     *
     * @param returnVaule 方法返回值
     * @param mode        调用点
     */
    public static void after(Object returnVaule, int mode, int nHash, int nHashA, int nHashB, String desc, Object instanceOrClass, Object[] args, int logtype, Object params) {

//        MethodConfig config = manager.getConfig(nHash, nHashA, nHashB);
//
//        if (config == null) {
//            return;
//        }
//
//        if (config.isAfter()
//                && getMonitorContext().isInited()
//                && getMonitorContext().getTopArg()) {
//            try {
//                callMonitorThisMethodLuaFunc(LuaConstance.AFTER, returnVaule, mode);
//            } catch (LuaRunException e) {
//                //  2018/9/25 执行失败，log error
//                MLog.d(MONITOR, "run AFTER ", e);
//            }
//
//        }
//
//        //获取数据，清理执行栈
//        if (getMonitorContext().isInited()
//                && getMonitorContext().getTopArg()) {
//            //获取脚本中产生的数据，生成对应的数据
//            try {
//                Object object = callMonitorRootLuaFunc(LuaConstance.GETTOPDATA);
//                if (uploadService != null && manager != null) {
//                    try {
//                        uploadService.sendData(manager.generateData(config, object));
//                    } catch (Exception e) {
//                        MLog.d(MONITOR, "upload error", e);
//                    }
//                }
//            } catch (LuaRunException e) {
//                MLog.d(MONITOR, "run GETTOPDATA ", e);
//            }
//
//            try {
//                callMonitorRootLuaFunc(LuaConstance.RELEASETOP);
//            } catch (LuaRunException e) {
//                MLog.d(MONITOR, "run RELEASETOP ", e);
//            }
//            getMonitorContext().popTop();
//        }
    }

    /**
     * 转换成数组
     *
     * @param args
     * @return
     */
    private static Object getList(Object... args) {
        List list = new ArrayList();

        for (Object object : args) {
            if (object == null) {
                continue;
            }
            if (object instanceof Collection) {
                list.addAll((Collection) object);
            } else if (object instanceof Object[]) {
                for (Object objects : (Object[]) object) {
                    if (objects == null) {
                        continue;
                    } else {
                        list.add(objects);
                    }
                }
            } else {
                list.add(object);
            }
        }

        return list;
    }


    /**
     * 执行根的lua配置
     *
     * @param funcName
     * @param args
     * @return
     * @throws LuaRunException
     */
    private static Object callMonitorRootLuaFunc(String funcName, Object... args) throws LuaRunException {
        Object returnValie = null;
        LuaState luaState = getMonitorContext().getState();
        luaState.getGlobal(funcName);
        if (args.length == 0) {
            luaState.pushNil();
        } else {
            LuaUtils.pushDataToLua(luaState, getList(args));
        }
        int initResult = luaState.pcall(1, 1, 0);
        if (initResult == 0) {
            if (!luaState.isNil(-1)) {
                //返回值是个table,转换成map
                returnValie = LuaUtils.parseLuaTable(luaState);
            }
            luaState.pop(-1);
        } else {
            try {
                LuaRunException exception = null;
                try {
                    exception = new LuaRunException(initResult, 0, luaState.toJavaObject(-1), null);
                } catch (LuaException e) {

                }
                throw exception;
            } finally {
                luaState.pop(-1);
            }
        }
        return returnValie;
    }

    /**
     * 执行当前方法的lua配置
     *
     * @param funcName
     * @param args
     * @return
     * @throws LuaRunException
     */
    private static Object callMonitorThisMethodLuaFunc(String funcName, Object... args) throws LuaRunException {
        Object returnValie = null;
        LuaState luaState = getMonitorContext().getState();
        luaState.getGlobal(LuaConstance.CALLTOPFUNC);
        luaState.pushString(funcName);
        if (args.length == 0) {
            luaState.pushNil();
        } else {
            LuaUtils.pushDataToLua(luaState, getList(args));
        }
        int initResult = luaState.pcall(2, 2, 0);
        if (initResult == 0) {
            int code = luaState.toInteger(-1);
            luaState.pop(-1);
            if (code == 0) {
                //有返回值
                if (!luaState.isNil(-1)) {
                    //返回值是个table,转换成map
                    returnValie = LuaUtils.parseLuaTable(luaState);
                }
                luaState.pop(-1);
            } else {
                //执行出错
                try {
                    LuaRunException exception = new LuaRunException(0, code, null, luaState.toJavaObject(-2));
                    throw exception;
                } catch (LuaException e) {
                } finally {
                    luaState.pop(-1);
                }
            }
        } else {
            //Java调用lua出错
            try {
                LuaRunException exception = new LuaRunException(initResult, 0, luaState.toJavaObject(-1), null);
                throw exception;
            } catch (LuaException e) {
            } finally {
                luaState.pop(-1);
            }
        }
        return returnValie;
    }

    /**
     * @return
     */
    private static MonitorContext getMonitorContext() {

//        if (weakThread.get() == null) {
//            WeakReference reference = new WeakReference<>(Thread.currentThread());
//            MonitorContext luaThreadInfo = pools.obtain();
//            //初始化线程信息
//            luaThreadInfo.setThreadId(Thread.currentThread().getId());
//            luaThreadInfo.setThreadName(Thread.currentThread().getName());
//
//            ///初始化luastate
//            LuaState luaState = LuaStateFactory.newLuaState();
//            luaState.openLibs();
//            int reslut = luaState.LdoString(MethodConfig.getLuaRootString());
//
//            if (reslut == 0) {
//                luaThreadInfo.setState(luaState);
//            } else {
//                //初始化失败
//                luaThreadInfo.setState(null);
//                //记录log原因
//            }
//            luaMaps.put(reference, luaThreadInfo);
//            weakThread.set(reference);
//            return luaThreadInfo;
//        }
//        return luaMaps.get(weakThread.get());
        return null;
    }

}
