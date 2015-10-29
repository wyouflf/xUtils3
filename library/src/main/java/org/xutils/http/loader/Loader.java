package org.xutils.http.loader;


import org.xutils.cache.DiskCacheEntity;
import org.xutils.http.ProgressCallbackHandler;
import org.xutils.http.RequestParams;
import org.xutils.http.UriRequest;
import org.xutils.http.app.RequestTracker;

import java.io.InputStream;

/**
 * Author: wyouflf
 * Time: 2014/05/26
 */
public interface Loader<T> {

    Loader<T> newInstance();

    void setParams(final RequestParams params);

    void setProgressCallbackHandler(final ProgressCallbackHandler progressCallbackHandler);

    T load(final InputStream in) throws Throwable;

    T load(final UriRequest request) throws Throwable;

    T loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable;

    void save2Cache(final UriRequest request);

    void setResponseTracker(RequestTracker tracker);

    RequestTracker getResponseTracker();
}
