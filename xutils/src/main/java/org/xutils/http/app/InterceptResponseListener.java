package org.xutils.http.app;


import org.xutils.http.request.UriRequest;

/**
 * Created by wyouflf on 15/11/10.
 * 拦截请求响应, 在请求发送之后调用.
 */
public interface InterceptResponseListener {

    /**
     * 在后台线程工作,
     * 可以在这里获取header数据, 验证一些特殊的信息等,
     * 如果有验证错误可以在这里抛出错误.
     *
     * @param request
     */
    void intercept(UriRequest request) throws Throwable;

}
