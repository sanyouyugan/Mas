package com.qiudaoyu.monitor.analysis.network.http;

/**
 * 创建时间: 2018/12/5
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 * http请求流程
 * <p>
 * https://upload-images.jianshu.io/upload_images/852671-91939d53b0f3957b.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/813/format/webp
 * <p>
 * 通过AOP方式拦截对应的网络请求API实现统计
 * <p>
 * 例如：
 * <p>
 * AspectJ的实现
 * @Pointcut("target(java.net.URLConnection) && " +
 * "!within(retrofit.appengine.UrlFetchClient) " +
 * "&& !within(okio.Okio) && !within(butterknife.internal.ButterKnifeProcessor) " +
 * "&& !within(com.flurry.sdk.hb)" +
 * "&& !within(rx.internal.util.unsafe.*) " +
 * "&& !within(net.sf.cglib..*)" +
 * "&& !within(com.huawei.android..*)" +
 * "&& !within(com.sankuai.android.nettraffic..*)" +
 * "&& !within(roboguice..*)" +
 * "&& !within(com.alipay.sdk..*)")
 * protected void baseCondition() {
 * <p>
 * }
 * @Pointcut("call (org.apache.http.HttpResponse org.apache.http.client.HttpClient.execute ( org.apache.http.client.methods.HttpUriRequest))"
 * + "&& target(org.apache.http.client.HttpClient)"
 * + "&& args(request)"
 * + "&& !within(com.sankuai.android.nettraffic.factory..*)"
 * + "&& baseClientCondition()"
 * )
 * void httpClientExecute(HttpUriRequest request) {
 * <p>
 * }
 */
public class HttpStats {





}
