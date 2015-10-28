package org.xutils.http;


import org.json.JSONException;
import org.xutils.common.Callback;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.HttpException;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashSet;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
public final class HttpRetryHandler {

    private int maxRetryCount;

    private static HashSet<Class<?>> blackList = new HashSet<Class<?>>();

    static {
        blackList.add(HttpException.class);
        blackList.add(Callback.CancelledException.class);
        blackList.add(MalformedURLException.class);
        blackList.add(URISyntaxException.class);
        blackList.add(NoRouteToHostException.class);
        blackList.add(PortUnreachableException.class);
        blackList.add(ProtocolException.class);
        blackList.add(NullPointerException.class);
        blackList.add(FileNotFoundException.class);
        blackList.add(JSONException.class);
        blackList.add(SocketTimeoutException.class);
        blackList.add(UnknownHostException.class);
        blackList.add(IllegalArgumentException.class);
    }

    public HttpRetryHandler() {
        this(2);
    }

    public HttpRetryHandler(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    boolean retryRequest(Throwable ex, int count, UriRequest request) {

        if (count > maxRetryCount || request == null) {
            LogUtil.w("The Max Retry times has been reached!");
            LogUtil.w(ex.getMessage(), ex);
            return false;
        }

        if (request.getParams().getMethod() != HttpMethod.GET) {
            LogUtil.w("The http method is not HTTP GET! The NetWork operation can not be retried.");
            LogUtil.w(ex.getMessage(), ex);
            return false;
        }

        if (blackList.contains(ex.getClass())) {
            LogUtil.w("The NetWork operation can not be retried.");
            LogUtil.w(ex.getMessage(), ex);
            return false;
        }

        return true;
    }
}
