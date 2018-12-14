package com.qiudaoyu.monitor.log.dynamiclog.lua;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.qiudaoyu.monitor.utils.AppUtils;
import com.qiudaoyu.monitor.utils.AppUtils;

import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

/**
 * 创建时间: 2018/10/15
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class TestJavaLua {

    public static void testLua(String[] args, Context context, TextView tx) {
        LuaState state = LuaStateFactory.newLuaState();
        //加载Lua 自身类库
        state.openLibs();

        String root = AppUtils.getStringContentFromAssets("root.lua", context);
        String test = AppUtils.getStringContentFromAssets("test.lua", context);
        state.LdoString(root);
        state.LdoString(test);
        state.getGlobal("callTopFunc");
        state.pushString("before");
        state.pushJavaObject(tx);
        state.pcall(2, 2, 0);
        Log.e("1", "1");

    }


}
