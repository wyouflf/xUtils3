package org.xutils.http.app;

import org.xutils.common.util.LogUtil;
import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpRequest;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by wyouflf on 15/8/20.
 * 默认参数构造器
 */
public class DefaultParamsBuilder implements ParamsBuilder {

    public DefaultParamsBuilder() {
    }

    /**
     * 根据@HttpRequest构建请求的url
     *
     * @param params
     * @param httpRequest
     * @return
     */
    @Override
    public String buildUri(RequestParams params, HttpRequest httpRequest) {
        return httpRequest.host() + "/" + httpRequest.path();
    }

    /**
     * 根据注解的cacheKeys构建缓存的自定义key,
     * 如果返回null, 默认使用 url 和整个 query string 组成.
     *
     * @param params
     * @param cacheKeys
     * @return
     */
    @Override
    public String buildCacheKey(RequestParams params, String[] cacheKeys) {
        String cacheKey = null;
        if (cacheKeys != null && cacheKeys.length > 0) {

            cacheKey = params.getUri() + "?";

            // 添加cacheKeys对应的参数
            for (String key : cacheKeys) {
                String value = params.getStringParameter(key);
                if (value != null) {
                    cacheKey += key + "=" + value + "&";
                }
            }
        }
        return cacheKey;
    }

    /**
     * 自定义SSLSocketFactory
     *
     * @return
     */
    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return getTrustAllSSLSocketFactory();
    }

    /**
     * 为请求添加通用参数等操作
     *
     * @param params
     */
    @Override
    public void buildParams(RequestParams params) {
    }

    /**
     * 自定义参数签名
     *
     * @param params
     * @param signs
     */
    @Override
    public void buildSign(RequestParams params, String[] signs) {

    }

    private static SSLSocketFactory trustAllSSlSocketFactory;

    public static SSLSocketFactory getTrustAllSSLSocketFactory() {
        if (trustAllSSlSocketFactory == null) {
            synchronized (DefaultParamsBuilder.class) {
                if (trustAllSSlSocketFactory == null) {

                    // 信任所有证书
                    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }};
                    try {
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, trustAllCerts, null);
                        trustAllSSlSocketFactory = sslContext.getSocketFactory();
                    } catch (Throwable ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                }
            }
        }

        return trustAllSSlSocketFactory;
    }

}
