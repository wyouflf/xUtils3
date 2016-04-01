package org.xutils.http;

import org.xutils.common.util.LogUtil;
import org.xutils.http.app.RequestTracker;
import org.xutils.http.request.UriRequest;

/**
 * Created by wyouflf on 15/11/4.
 * Wrapper for tracker
 */
/*package*/ final class RequestTrackerWrapper implements RequestTracker {

    private final RequestTracker base;

    public RequestTrackerWrapper(RequestTracker base) {
        this.base = base;
    }

    @Override
    public void onWaiting(RequestParams params) {
        try {
            base.onWaiting(params);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void onStart(RequestParams params) {
        try {
            base.onStart(params);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void onRequestCreated(UriRequest request) {
        try {
            base.onRequestCreated(request);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void onCache(UriRequest request, Object result) {
        try {
            base.onCache(request, result);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void onSuccess(UriRequest request, Object result) {
        try {
            base.onSuccess(request, result);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void onCancelled(UriRequest request) {
        try {
            base.onCancelled(request);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void onError(UriRequest request, Throwable ex, boolean isCallbackError) {
        try {
            base.onError(request, ex, isCallbackError);
        } catch (Throwable exOnError) {
            LogUtil.e(exOnError.getMessage(), exOnError);
        }
    }

    @Override
    public void onFinished(UriRequest request) {
        try {
            base.onFinished(request);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }
}
