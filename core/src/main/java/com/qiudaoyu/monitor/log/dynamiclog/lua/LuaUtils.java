package com.qiudaoyu.monitor.log.dynamiclog.lua;

import org.keplerproject.luajava.LuaState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建时间: 2018/9/25
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class LuaUtils {
    /**
     * 参数将map/list数据入lua栈，
     *
     * @param mLuaState lua
     * @param obj       map or list data
     */
    public static void pushDataToLua(LuaState mLuaState, Object obj) {
        try {
            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                // 创建一个新表， lua 中存储数据的结果为表
                mLuaState.newTable();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    mLuaState.pushString(entry.getKey());
                    mLuaState.pushObjectValue(entry.getValue());
                    mLuaState.setTable(-3);
                }
            } else if (obj instanceof List) {
                List<Object> list = (List<Object>) obj;
                //键表
                mLuaState.createTable(0, 0);
                for (int i = 0; i < list.size(); i++) {
                    //将数组中的第 i个数据入栈
                    mLuaState.pushObjectValue(list.get(i));
                    //将刚刚入栈在栈顶的数据存入表中（-2 表示表在栈内的位置，（i+1)表示数据在表中存储位置），同时这个数据会自动从栈顶pop
                    mLuaState.rawSetI(-2, i + 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // LuaUtil 中代码，解析 lua 表中数据
    public static Object  parseLuaTable(LuaState luaState) {
        Object luaData = null;
//        取出栈里面元素个数
        int top = luaState.getTop();
//        判断栈定是否为表元素
        if (luaState.isTable(top)) {
//            向栈顶部 push 一个 nil 元素
            luaState.pushNil();
            while (luaState.next(top) != 0) {
//                key type
//                -2 位置为 key
                int keyType = luaState.type(-2);
                String keyName = luaState.typeName(keyType);
//                -1 位置为值，即栈顶了
                int valueType = luaState.type(-1);
                String valueName = luaState.typeName(valueType);
                //数组
                if (keyName.equals("number")) {
                    if (luaData == null) {
                        luaData = new ArrayList<>();
                    }
                    if (luaData instanceof List) {
                        parseTableValueList(luaState, valueName, (List<Object>) luaData);
                    } else {
                        parseTableValueMap(luaState, valueName, (Map<String, Object>) luaData);
                    }

                    //map 键值对
                } else if (keyName.equals("string")) {
                    if (luaData == null) {
                        luaData = new HashMap<>();
                    }
                    if (luaData instanceof Map) {
                        parseTableValueMap(luaState, valueName, (Map<String, Object>) luaData);
                    } else {
                        parseTableValueList(luaState, valueName, (List<Object>) luaData);
                    }
                }
                if (!"function".equals(valueName)) {
                    luaState.pop(1);
                }
            }
        }
        return luaData;
    }

    //解析 map 结构
    private static void parseTableValueMap(LuaState mLuaState, String valueName, Map<String, Object> objectMap) {
        switch (valueName) {
            case "null":
                break;
            case "number":
                objectMap.put(mLuaState.toString(-2), mLuaState.toNumber(-1));
                break;
            case "string":
                objectMap.put(mLuaState.toString(-2), mLuaState.toString(-1));
                break;
            case "boolean":
                objectMap.put(mLuaState.toString(-2), mLuaState.toBoolean(-1));
                break;
            case "int":
            case "integer":
                objectMap.put(mLuaState.toString(-2), mLuaState.toInteger(-1));
                break;
            case "function":
                String key = mLuaState.toString(-2);
                int value = mLuaState.Lref(LuaState.LUA_REGISTRYINDEX);
                objectMap.put(key, value);
                break;
            case "table":
                objectMap.put(mLuaState.toString(-2), parseLuaTable(mLuaState));
                break;
            default:
                break;
        }
    }

    // 解析数组结构
    private static void parseTableValueList(LuaState mLuaState, String valueName, List<Object> objectMap) {
        switch (valueName) {
            case "null":
                break;
            case "number":
                objectMap.add(mLuaState.toNumber(-1));
                break;
            case "string":
                objectMap.add(mLuaState.toString(-1));
                break;
            case "boolean":
                objectMap.add(mLuaState.toBoolean(-1));
                break;
            case "int":
            case "integer":
                objectMap.add(mLuaState.toInteger(-1));
                break;
            case "function":
                int value = mLuaState.Lref(LuaState.LUA_REGISTRYINDEX);
                objectMap.add(value);
                break;
            case "table":
                objectMap.add(parseLuaTable(mLuaState));
                break;
            default:
                break;
        }
    }

    /**
     * push android map data to lua
     */
    private void pushAndroidMapDataToLua(LuaState luaState, String fun) {
        luaState.getGlobal("getMapDataFromAndroid");
        Map<String, String> mapDta = new HashMap<>();
        mapDta.put("formAndroid1", "I am from Android map1");
        mapDta.put("formAndroid2", "I am from Android map2");
        mapDta.put("formAndroid3", "I am from Android map3");
        pushDataToLua(luaState, mapDta);
    }


    //java 代码，调用 lua 函数

    private void getDataFromLua(LuaState luaState) {
        luaState.getGlobal("pushDataToAndroid");
        luaState.call(0, 0);
    }

}
