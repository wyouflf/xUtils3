package org.xutils.http.app;

import org.xutils.common.util.KeyValue;
import org.xutils.common.util.LogUtil;
import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpRequest;

import java.security.cert.X509Certificate;
import java.util.List;

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
     */
    @Override
    public String buildUri(RequestParams params, HttpRequest httpRequest) throws Throwable {
        return httpRequest.host() + "/" + httpRequest.path();
    }

    /**
     * 根据注解的cacheKeys构建缓存的自定义key,
     * 如果返回为空, 默认使用 url 和整个 query string 组成.
     */
    @Override
    public String buildCacheKey(RequestParams params, String[] cacheKeys) {
        StringBuilder result = new StringBuilder();
        if (cacheKeys != null && cacheKeys.length > 0) {
            result.append(params.getUri()).append("?");

            // 添加cacheKeys对应的参数
            for (String key : cacheKeys) {
                List<KeyValue> kvList = params.getParams(key);
                if (kvList != null && !kvList.isEmpty()) {
                    for (KeyValue kv : kvList) {
                        String value = kv.getValueStrOrNull();
                        if (value != null) {
                            result.append(key).append("=").append(value).append("&");
                        }
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * 自定义SSLSocketFactory
     */
    @Override
    public SSLSocketFactory getSSLSocketFactory() throws Throwable {
        return getTrustAllSSLSocketFactory();
    }

    /**
     * 为请求添加通用参数等操作
     */
    @Override
    public void buildParams(RequestParams params) throws Throwable {
    }

    /**
     * 自定义参数签名
     */
    @Override
    public void buildSign(RequestParams params, String[] signs) throws Throwable {

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
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            LogUtil.d("checkClientTrusted:" + authType);
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            LogUtil.d("checkServerTrusted:" + authType);
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
