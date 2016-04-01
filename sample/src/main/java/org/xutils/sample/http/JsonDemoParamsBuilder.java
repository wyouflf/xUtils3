package org.xutils.sample.http;

import android.text.TextUtils;

import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpRequest;
import org.xutils.http.app.ParamsBuilder;
import org.xutils.x;

import java.util.HashMap;

import javax.net.ssl.SSLSocketFactory;

/**
 * Created by wyouflf on 16/1/23.
 */
public class JsonDemoParamsBuilder implements ParamsBuilder {

    public static final String SEEVER_A = "a";
    public static final String SEEVER_B = "b";

    private static final HashMap<String, String> SERVER_MAP = new HashMap<String, String>();

    private static final HashMap<String, String> DEBUG_SERVER_MAP = new HashMap<String, String>();

    static {
        SERVER_MAP.put(SEEVER_A, "http://a.xxx.xxx");
        SERVER_MAP.put(SEEVER_B, "http://b.xxx.xxx");
        DEBUG_SERVER_MAP.put(SEEVER_A, "http://debug.a.xxx.xxx");
        DEBUG_SERVER_MAP.put(SEEVER_B, "http://debug.b.xxx.xxx");
    }

    @Override
    public String buildUri(RequestParams params, HttpRequest httpRequest) {
        String url = getHost(httpRequest.host());
        url += "/" + httpRequest.path();
        return url;
    }

    @Override
    public String buildCacheKey(RequestParams params, String[] cacheKeys) {
        return null;
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return null;
    }

    @Override
    public void buildParams(RequestParams params) {
        // 添加公共参数
        params.addParameter("common_a", "xxxx");
        params.addParameter("common_b", "xxxx");


        // 将post请求的body参数以json形式提交
        params.setAsJsonContent(true);
        // 或者query参数和body参数都json形式
        /*String json = params.toJSONString();
        params.clearParams();// 清空参数
        if (params.getMethod() == HttpMethod.GET) {
            params.addQueryStringParameter("xxx", json);
        } else {
            params.setBodyContent(json);
        }*/
    }

    @Override
    public void buildSign(RequestParams params, String[] signs) {
        params.addParameter("sign", "xxxx");
    }


    private String getHost(String host) {
        String result = null;
        if (x.isDebug()) {
            result = DEBUG_SERVER_MAP.get(host);
        } else {
            result = SERVER_MAP.get(host);
        }
        return TextUtils.isEmpty(result) ? host : result;
    }
}
