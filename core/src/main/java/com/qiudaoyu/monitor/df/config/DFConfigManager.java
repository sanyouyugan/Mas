package com.qiudaoyu.monitor.df.config;

import android.content.Context;

import com.qiudaoyu.monitor.log.dynamiclog.config.ConfigManager;
import com.qiudaoyu.monitor.log.dynamiclog.config.MethodConfig;
import com.qiudaoyu.monitor.log.dynamiclog.config.ConfigManager;
import com.qiudaoyu.monitor.log.dynamiclog.config.MethodConfig;

/**
 * 创建时间: 2018/9/26
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class DFConfigManager extends ConfigManager {

    private DFConfigManager() {

    }

    public static DFConfigManager getInstance() {
        return Single.mInstance;
    }

    @Override
    public Object generateData(MethodConfig config, Object data) {

        return null;
    }

    @Override
    public void queryConfigs(Context context) {


    }

    public static class Single {
        static DFConfigManager mInstance = new DFConfigManager();
    }

}
