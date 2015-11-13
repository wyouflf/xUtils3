package org.xutils.http.app;

import org.xutils.http.request.UriRequest;

/**
 * Created by wyouflf on 15/9/10.
 * 请求过程追踪, 适合用来记录请求日志.
 * 所有回调方法都在主线程进行.
 * <p>
 * 用法:
 * 1. 请的callback参数同时实现RequestTracker;
 * 2. 自定义的ResponseParser实现RequestTracker;
 * 3. 注册给LoaderFactory.
 * 注意: 请求回调RequestTracker时优先级按照上面的顺序,
 * 找到一个RequestTracker的实现会忽略其他.
 */
public interface RequestTracker {

    void onWaiting(UriRequest request);

    void onStart(UriRequest request);

    void onCache(UriRequest request);

    void onSuccess(UriRequest request);

    void onCancelled(UriRequest request);

    void onError(UriRequest request, Throwable ex, boolean isCallbackError);

    void onFinished(UriRequest request);
}
