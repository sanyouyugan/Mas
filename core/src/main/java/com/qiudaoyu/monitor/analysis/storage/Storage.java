package com.qiudaoyu.monitor.analysis.storage;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/**
 * 创建时间: 2018/12/4
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class Storage {
    public long[] getSDCardMemory() {
        long[] sdCardInfo = new long[2];
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            long bSize = sf.getBlockSize();
            long bCount = sf.getBlockCount();
            long availBlocks = sf.getAvailableBlocks();
            sdCardInfo[0] = bSize * bCount;//总大小
            sdCardInfo[1] = bSize * availBlocks;//可用大小
        }
        return sdCardInfo;
    }
}
