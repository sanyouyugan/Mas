package com.qiudaoyu.monitor.upload;


/**
 * 创建时间: 2018/9/26
 * 类描述:
 * 上传日志类
 *
 * @author 秋刀鱼
 * @version 1.0
 */

public interface UploadServiceInterface<T> {

    void initialize(T arg);


    void sendData(Object data);

    void setForground(boolean isForgroud);

    void shutDown();
}
