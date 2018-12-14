package com.qiudaoyu.test;

import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 创建时间: 2017/8/22 下午2:50
 * 类描述:
 *
 * @author 木棉
 */
public interface LoginApi {

    @POST("?method=com.dfire.boss.center.soa.login.service.IUnifiedLoginClientService.login")
    rx.Observable<HttpResult> doLogin(@Query("param_str") String param_str, @Query("base_param") String base_param);

}
