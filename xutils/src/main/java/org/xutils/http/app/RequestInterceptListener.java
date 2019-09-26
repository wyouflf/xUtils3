package org.xutils.http.app;


import org.xutils.http.request.UriRequest;

/**
 * Created by wyouflf on 15/11/10.
 * 拦截请求响应(在后台线程工作).
 * <p>
 * 用法:
 * 1. 请求的callback参数同时实现RequestInterceptListener
 * 2. 或者使用 @HttpRequest 注解实现ParamsBuilder接口
 */
public interface RequestInterceptListener {

    /**
     * 检查请求参数等处理
     */
    void beforeRequest(UriRequest request) throws Throwable;

    /**
     * 检查请求相应头等处理
     */
    void afterRequest(UriRequest request) throws Throwable;
}