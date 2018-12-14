package com.qiudaoyu.test;

import com.qiudaoyu.monitor.log.mlog.annotation.MAop;
import com.qiudaoyu.monitor.log.mlog.annotation.MAop;

/**
 * 创建时间: 2018/11/29
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class Test {

    @MAop
    private long test1(long x, long x1, long x2, long x3) {
        testl(x);
        testl(x1);
        testl(x2);
        return x2;
    }

    private long testl(long x) {

        return x;
    }

    private int testi(int x) {

        return x;
    }


}
