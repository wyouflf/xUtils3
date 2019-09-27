package org.xutils.http.app;

import android.text.TextUtils;
import android.webkit.URLUtil;

import org.xutils.http.RequestParams;
import org.xutils.http.request.UriRequest;

public class DefaultRedirectHandler implements RedirectHandler {
    @Override
    public RequestParams getRedirectParams(UriRequest request) throws Throwable {
        org.xutils.http.request.HttpRequest httpRequest = null;
        RequestParams params = null;
        if (request instanceof org.xutils.http.request.HttpRequest) {
            httpRequest = (org.xutils.http.request.HttpRequest) request;
            params = httpRequest.getParams();
            String location = httpRequest.getResponseHeader("Location");
            if (!TextUtils.isEmpty(location)) {
                if (!URLUtil.isHttpsUrl(location) && !URLUtil.isHttpUrl(location)) {
                    String url = params.getUri();
                    if (location.startsWith("/")) {
                        int pathIndex = url.indexOf("/", 8);
                        if (pathIndex != -1) {
                            url = url.substring(0, pathIndex);
                        }
                    } else {
                        int pathIndex = url.lastIndexOf("/");
                        if (pathIndex >= 8) {
                            url = url.substring(0, pathIndex + 1);
                        } else {
                            url += "/";
                        }
                    }
                    location = url + location;
                }
                params.setUri(location);
                return params;
            }
        }

        return null;
    }
}
