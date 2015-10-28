package org.xutils.http.app;

import org.xutils.http.UriRequest;

/**
 * Created by wyouflf on 15/9/10.
 */
public interface ResponseTracker {

    void onStart(UriRequest request);

    void onSuccess(UriRequest request);

    void onCancelled(UriRequest request);

    void onError(UriRequest request, Throwable ex, boolean isCallbackError);

}
