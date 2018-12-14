package com.qiudaoyu.test;


import com.google.gson.Gson;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


public class RetrofitService {

    /**
     * 连接超时时间
     * 5s
     */
    public static final int CONNECT_TIMEOUT_MILLIS = 6 * 1000;
    /**
     * 响应超时时间
     * 5s
     */
    public static final int READ_TIMEOUT_MILLIS = 6 * 1000;
    private final static Object mRetrofitLock = new Object();

    private static Retrofit sRetrofit;
    private static OkHttpClient sOkHttpClient;
    private static ConcurrentHashMap<Class<?>, Object> mCache = new ConcurrentHashMap();

    public static Retrofit getRetrofit() {
        if (sRetrofit == null) {
            synchronized (mRetrofitLock) {
                if (sRetrofit == null) {
                    OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder();
                    HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
                    clientBuilder
                            .connectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                            .readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    clientBuilder.addInterceptor(httpLoggingInterceptor);
                    sOkHttpClient = clientBuilder.build();
                    sRetrofit = new Retrofit.Builder().client(sOkHttpClient)
                            .baseUrl("http://www.baidu.com")
                            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                            .addConverterFactory(GsonConverterFactory.create(new Gson()))
                            .build();
                }
            }
        }
        return sRetrofit;
    }


    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Class<T> tClass) {
        return getRetrofit().create(tClass);
    }

}

