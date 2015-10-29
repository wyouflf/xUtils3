package org.xutils.http.loader;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.http.ProgressCallbackHandler;
import org.xutils.http.RequestParams;
import org.xutils.http.UriRequest;
import org.xutils.http.app.RequestTracker;

import java.io.InputStream;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
/*package*/ class BooleanLoader implements Loader<Boolean> {

    @Override
    public Loader<Boolean> newInstance() {
        return new BooleanLoader();
    }

    @Override
    public void setParams(final RequestParams params) {

    }

    @Override
    public void setProgressCallbackHandler(final ProgressCallbackHandler progressCallbackHandler) {

    }

    @Override
    public Boolean load(final InputStream in) throws Throwable {
        return false;
    }

    @Override
    public Boolean load(final UriRequest request) throws Throwable {
        request.sendRequest();
        return request.getResponseCode() < 300;
    }

    @Override
    public Boolean loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable {
        return null;
    }

    @Override
    public void save2Cache(final UriRequest request) {

    }

    private RequestTracker tracker;

    @Override
    public void setResponseTracker(RequestTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public RequestTracker getResponseTracker() {
        return tracker;
    }
}
