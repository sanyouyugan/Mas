package com.qiudaoyu.monitor.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 *
 */

public class IoUtil {
    public static final int DEFAULT_BUFFER_SIZE = 32768;

    private IoUtil() {
    }

    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException var2) {
            }
        }
    }
}
