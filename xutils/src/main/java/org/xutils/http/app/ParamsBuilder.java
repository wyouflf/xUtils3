package org.xutils.http.app;

import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpRequest;

import javax.net.ssl.SSLSocketFactory;

/**
 * Created by wyouflf on 15/8/20.
 * <p>
 * {@link org.xutils.http.annotation.HttpRequest} 注解的参数构建的模板接口
 */
public interface ParamsBuilder {

    /**
     * 根据@HttpRequest构建请求的url
     */
    String buildUri(RequestParams params, HttpRequest httpRequest) throws Throwable;

    /**
     * 根据注解的cacheKeys构建缓存的自定义key,
     * 如果返回为空, 默认使用 url 和整个 query string 组成.
     */
    String buildCacheKey(RequestParams params, String[] cacheKeys);

    /**
     * 自定义SSLSocketFactory
     */
    SSLSocketFactory getSSLSocketFactory() throws Throwable;

    /**
     * 为请求添加通用参数等操作
     */
    void buildParams(RequestParams params) throws Throwable;

    /**
     * 自定义参数签名
     */
    void buildSign(RequestParams params, String[] signs) throws Throwable;
}
