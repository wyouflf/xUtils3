package org.xutils;

import org.xutils.common.Callback;
import org.xutils.http.HttpMethod;
import org.xutils.http.RequestParams;

/**
 * Created by wyouflf on 15/6/17.
 * http请求接口
 */
public interface HttpManager {

    /**
     * 异步GET请求
     *
     * @param entity 请求参数
     * @param callback 请求回调
     * @return {@link org.xutils.common.Callback.Cancelable}
     */
    <T> Callback.Cancelable get(RequestParams entity, Callback.CommonCallback<T> callback);

    /**
     * 异步POST请求
     *
     * @param entity 请求参数
     * @param callback 请求回调
     * @return {@link org.xutils.common.Callback.Cancelable}
     */
    <T> Callback.Cancelable post(RequestParams entity, Callback.CommonCallback<T> callback);

    /**
     * 异步请求
     *
     * @param method {@link HttpMethod}
     * @param entity 请求参数
     * @param callback 请求回调
     * @return {@link org.xutils.common.Callback.Cancelable}
     */
    <T> Callback.Cancelable request(HttpMethod method, RequestParams entity, Callback.CommonCallback<T> callback);


    /**
     * 同步GET请求
     *
     * @param entity 请求参数
     * @param resultType 返回结果类型 (默认只支持{@link org.xutils.http.loader.LoaderFactory}中注册类型)
     * @return 返回数据
     * @throws Throwable
     */
    <T> T getSync(RequestParams entity, Class<T> resultType) throws Throwable;

    /**
     * 同步POST请求
     *
     * @param entity 请求参数
     * @param resultType 返回结果类型 (默认只支持{@link org.xutils.http.loader.LoaderFactory}中注册类型)
     * @return 返回数据
     * @throws Throwable
     */
    <T> T postSync(RequestParams entity, Class<T> resultType) throws Throwable;

    /**
     * 同步请求
     *
     * @param method {@link HttpMethod}
     * @param entity 请求参数
     * @param resultType 返回结果类型 (默认只支持{@link org.xutils.http.loader.LoaderFactory}中注册类型)
     * @return 返回数据
     * @throws Throwable
     */
    <T> T requestSync(HttpMethod method, RequestParams entity, Class<T> resultType) throws Throwable;

    /**
     * 同步请求
     *
     * @param method {@link HttpMethod}
     * @param entity 请求参数
     * @param callback 自定义返回类型
     * @return 返回数据
     * @throws Throwable
     */
    <T> T requestSync(HttpMethod method, RequestParams entity, Callback.TypedCallback<T> callback) throws Throwable;
}
