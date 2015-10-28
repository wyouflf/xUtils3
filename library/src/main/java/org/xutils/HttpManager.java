package org.xutils;

import org.xutils.common.Callback;
import org.xutils.http.HttpMethod;
import org.xutils.http.RequestParams;

/**
 * Created by wyouflf on 15/6/17.
 * http请求接口
 */
public interface HttpManager {

    <T> Callback.Cancelable get(RequestParams entity, Callback.CommonCallback<T> callback);

    <T> Callback.Cancelable post(RequestParams entity, Callback.CommonCallback<T> callback);

    <T> Callback.Cancelable request(HttpMethod method, RequestParams entity, Callback.CommonCallback<T> callback);


    <T> T getSync(RequestParams entity, Class<T> resultType) throws Throwable;

    <T> T postSync(RequestParams entity, Class<T> resultType) throws Throwable;

    <T> T requestSync(HttpMethod method, RequestParams entity, Class<T> resultType) throws Throwable;

}
