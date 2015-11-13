package org.xutils.http.app;

import android.text.TextUtils;

import org.xutils.common.util.LogUtil;
import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpRequest;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

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

    @Override
    public String buildUri(HttpRequest httpRequest) {
        return httpRequest.host() + "/" + httpRequest.path();
    }

    @Override
    public String buildCacheKey(RequestParams params, String[] cacheKeys) {
        String cacheKey = params.getUri() + "@";
        if (cacheKeys != null && cacheKeys.length > 0) {
            // 添加cacheKeys对应的queryParams
            HashMap<String, String> queryParams = params.getQueryStringParams();
            if (queryParams != null) {
                for (String key : cacheKeys) {
                    String value = queryParams.get(key);
                    if (!TextUtils.isEmpty(value)) {
                        cacheKey += key + "=" + value + "&";
                    }
                }
            }
        } else {
            // 添加所有queryParams
            HashMap<String, String> queryParams = params.getQueryStringParams();
            if (queryParams != null) {
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    String name = entry.getKey();
                    String value = entry.getValue();
                    if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                        cacheKey += name + "=" + value + "&";
                    }
                }
            }
        }
        return cacheKey;
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return getTrustAllSSLSocketFactory();
    }

    @Override
    public void buildParams(RequestParams params) {
    }

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
