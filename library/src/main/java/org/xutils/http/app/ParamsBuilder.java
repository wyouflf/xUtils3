package org.xutils.http.app;

import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpRequest;

import javax.net.ssl.SSLSocketFactory;

/**
 * Created by wyouflf on 15/8/20.
 */
public interface ParamsBuilder {
    String buildUri(HttpRequest httpRequest);

    String buildCacheKey(RequestParams params, String[] cacheKeys);

    SSLSocketFactory getSSLSocketFactory();

    void buildParams(RequestParams params);

    void buildSign(RequestParams params, String[] signs);
}
