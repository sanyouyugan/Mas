package com.qiudaoyu.monitor.analysis.network.http.okhttp;

import android.text.TextUtils;

import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.MonitorData;
import com.qiudaoyu.monitor.analysis.MasData;
import com.qiudaoyu.monitor.analysis.metric.MetricListener;
import com.qiudaoyu.monitor.analysis.network.http.HttpInfo;
import com.qiudaoyu.monitor.log.MLog;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * 网络拦截器
 */
public class NetWorkInterceptor implements Interceptor {

    private static final String TAG = "Okhttp";

    private HttpInfo mOkHttpData;
    private MetricListener metricListener;

    public NetWorkInterceptor(MetricListener metricListener) {
        this.metricListener = metricListener;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        long startNs = System.currentTimeMillis();
        mOkHttpData = new HttpInfo();
        mOkHttpData.startTime = startNs;

        String trackId = MasData.getInstance().generatTrackId();
        //增加trackId
        Request original = chain.request();
        Request.Builder requestBuilder = original.newBuilder();
        requestBuilder.addHeader("trackId", trackId);
        mOkHttpData.setTrackId(trackId);
        Request newReq = requestBuilder.build();
        recordRequest(newReq);

        //记录结果
        try {
            Response response = chain.proceed(newReq);
            mOkHttpData.costTime = System.currentTimeMillis() - startNs;
            recordResponse(response);
            return response;
        } catch (Exception e) {
            mOkHttpData.costTime = System.currentTimeMillis() - startNs;
            mOkHttpData.setException(e);
            MLog.e(TAG, "HTTP FAILED: " + e);
            throw e;
        } finally {
            if (metricListener != null) {
                metricListener.metric(MonitorData.TYPE_METRIC_HTTP, mOkHttpData);
            }
        }
    }

    /**
     * request
     */
    private void recordRequest(Request request) {
        if (request == null || request.url() == null || TextUtils.isEmpty(request.url().toString())) {
            return;
        }

        mOkHttpData.url = request.url().toString();

        RequestBody requestBody = request.body();
        if (requestBody == null) {
            mOkHttpData.requestSize = request.url().toString().getBytes().length;
            MLog.d(TAG, "okhttp request 上行数据，大小：" + mOkHttpData.requestSize);
            return;
        }

        long contentLength = 0;
        try {
            contentLength = requestBody.contentLength();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (contentLength > 0) {
            mOkHttpData.requestSize = contentLength;
        } else {
            mOkHttpData.requestSize = request.url().toString().getBytes().length;
        }
    }

    /**
     * 设置 code responseSize
     */
    private void recordResponse(Response response) {
        if (response == null) {
            return;
        }

        mOkHttpData.code = response.code();

        MLog.d(TAG, "okhttp chain.proceed 状态码：" + mOkHttpData.code);

        if (!response.isSuccessful()) {
            return;
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return;
        }

        long contentLength = responseBody.contentLength();

        if (contentLength > 0) {
            MLog.d(TAG, "直接通过responseBody取到contentLength:" + contentLength);
        } else {
            BufferedSource source = responseBody.source();
            if (source != null) {
                try {
                    source.request(Long.MAX_VALUE);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Buffer buffer = source.buffer();
                contentLength = buffer.size();

                MLog.d(TAG, "通过responseBody.source()才取到contentLength:" + contentLength);
            }
        }

        mOkHttpData.responseSize = contentLength;
        MLog.d(TAG, "okhttp 接收字节数：" + mOkHttpData.responseSize);
    }
}