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
     *
     * @param httpRequest
     * @return
     */
    String buildUri(HttpRequest httpRequest);

    /**
     * 根据注解的cacheKeys构建缓存的自定义key,
     * 如果返回null, 默认使用 url 和整个 query string 组成.
     *
     * @param params
     * @param cacheKeys
     * @return
     */
    String buildCacheKey(RequestParams params, String[] cacheKeys);

    /**
     * 自定义SSLSocketFactory
     *
     * @return
     */
    SSLSocketFactory getSSLSocketFactory();

    /**
     * 为请求添加通用参数等操作
     *
     * @param params
     */
    void buildParams(RequestParams params);

    /**
     * 自定义参数签名
     *
     * @param params
     * @param signs
     */
    void buildSign(RequestParams params, String[] signs);
}
