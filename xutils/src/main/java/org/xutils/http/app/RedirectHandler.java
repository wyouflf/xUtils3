package org.xutils.http.app;

import org.xutils.http.RequestParams;
import org.xutils.http.request.UriRequest;

/**
 * Created by wyouflf on 15/11/12.
 * 请求重定向控制接口
 */
public interface RedirectHandler {

    /**
     * 根据请求信息返回自定义重定向的请求参数
     *
     * @param request 原始请求
     * @return 返回不为null时进行重定向
     */
    RequestParams getRedirectParams(UriRequest request) throws Throwable;
}
