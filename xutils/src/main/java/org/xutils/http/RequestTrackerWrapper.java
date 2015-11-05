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
    public void onWaiting(UriRequest request) {
        try {
            base.onWaiting(request);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void onStart(UriRequest request) {
        try {
            base.onStart(request);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void onCache(UriRequest request) {
        try {
            base.onCache(request);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void onSuccess(UriRequest request) {
        try {
            base.onSuccess(request);
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
