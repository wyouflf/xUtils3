package org.xutils.http.loader;


import org.xutils.cache.DiskCacheEntity;
import org.xutils.http.ProgressHandler;
import org.xutils.http.RequestParams;
import org.xutils.http.UriRequest;
import org.xutils.http.app.RequestTracker;

import java.io.InputStream;

/**
 * Author: wyouflf
 * Time: 2014/05/26
 */
public abstract class Loader<T> {

    protected RequestParams params;
    protected RequestTracker tracker;
    protected ProgressHandler progressHandler;

    public void setParams(final RequestParams params) {
        this.params = params;
    }

    public void setProgressHandler(final ProgressHandler callbackHandler) {
        this.progressHandler = callbackHandler;
    }

    public void setResponseTracker(RequestTracker tracker) {
        this.tracker = tracker;
    }

    public RequestTracker getResponseTracker() {
        return this.tracker;
    }

    public abstract Loader<T> newInstance();

    public abstract T load(final InputStream in) throws Throwable;

    public abstract T load(final UriRequest request) throws Throwable;

    public abstract T loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable;

    public abstract void save2Cache(final UriRequest request);
}
