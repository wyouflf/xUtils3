package org.xutils.http.app;

import org.xutils.http.request.UriRequest;

/**
 * Created by wyouflf on 15/9/10.
 */
public interface RequestTracker {

    void onWaiting(UriRequest request);

    void onStart(UriRequest request);

    void onCache(UriRequest request);

    void onSuccess(UriRequest request);

    void onCancelled(UriRequest request);

    void onError(UriRequest request, Throwable ex, boolean isCallbackError);

    void onFinished(UriRequest request);
}
