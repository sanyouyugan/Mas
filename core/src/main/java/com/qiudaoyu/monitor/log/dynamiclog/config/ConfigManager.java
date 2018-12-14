package com.qiudaoyu.monitor.log.dynamiclog.config;

import android.content.Context;

/**
 * 创建时间: 2018/9/18
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public abstract class ConfigManager {

    /**
     * 不依赖前值的多读一写的场景同步
     */
    private volatile ConfigOneWayHashTable table;

    private int version;

    public ConfigManager() {
        table = new ConfigOneWayHashTable();
    }

    /**
     *
     * @param nHash
     * @param nHashA
     * @param nHashB
     * @return
     */
    public MethodConfig getConfig(int nHash, int nHashA, int nHashB) {
        ConfigOneWayHashTable.HaseNode node = table.Hashed(nHash, nHashA, nHashB);
        if (node != null && node.data != null) {
            MethodConfig config = (MethodConfig) node.data;
            return config;
        }
        return null;
    }

    public abstract Object generateData(MethodConfig config, Object data);

    public abstract void queryConfigs(Context context);

    public void setNewConfigs(int version, ConfigOneWayHashTable.HaseNode[] nodes) {
        this.version = version;
        if (table != null) {
            table.setHashIndexTable(nodes);
        }
    }

    public void shutDown() {

    }

}
