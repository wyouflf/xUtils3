package org.xutils.http.loader;

import com.squareup.okhttp.Response;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.http.ProgressCallbackHandler;
import org.xutils.http.RequestParams;
import org.xutils.http.UriRequest;
import org.xutils.http.app.ResponseTracker;

import java.io.InputStream;

/**
 * Created by wyouflf on 15/8/18.
 */
/*package*/ class ResponseLoader implements Loader<Response> {
    @Override
    public Loader<Response> newInstance() {
        return new ResponseLoader();
    }

    @Override
    public void setParams(RequestParams params) {

    }

    @Override
    public void setProgressCallbackHandler(ProgressCallbackHandler progressCallbackHandler) {

    }

    @Override
    public Response load(InputStream in) throws Throwable {
        return null;
    }

    @Override
    public Response load(UriRequest request) throws Throwable {
        request.sendRequest();
        return request.getResponse();
    }

    @Override
    public Response loadFromCache(DiskCacheEntity cacheEntity) throws Throwable {
        return null;
    }

    @Override
    public void save2Cache(UriRequest request) {

    }

    private ResponseTracker tracker;

    @Override
    public void setResponseTracker(ResponseTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public ResponseTracker getResponseTracker() {
        return tracker;
    }
}
