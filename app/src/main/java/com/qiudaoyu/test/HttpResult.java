package com.qiudaoyu.test;

import android.support.annotation.Keep;


/**
 * 网络返回对象
 * Created by fengyu on 2016/9/28.
 */
@Keep
public class HttpResult<T> {

    private static final int SUCCESS = 1;

    private int code;

    private String errorCode;
    private String message;
    private String errorMessage;

    public HttpResult() {
    }
}
