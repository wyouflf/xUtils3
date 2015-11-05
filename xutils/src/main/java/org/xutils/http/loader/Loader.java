package org.xutils.http.loader;


import android.text.TextUtils;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.LruDiskCache;
import org.xutils.http.ProgressHandler;
import org.xutils.http.RequestParams;
import org.xutils.http.app.RequestTracker;
import org.xutils.http.request.UriRequest;

import java.io.InputStream;
import java.util.Date;

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

    protected void saveStringCache(UriRequest request, String resultStr) {
        if (!TextUtils.isEmpty(resultStr)) {
            DiskCacheEntity entity = new DiskCacheEntity();
            entity.setKey(request.getCacheKey());
            entity.setLastAccess(System.currentTimeMillis());
            entity.setEtag(request.getETag());
            entity.setExpires(request.getExpiration());
            entity.setLastModify(new Date(request.getLastModified()));
            entity.setTextContent(resultStr);
            LruDiskCache.getDiskCache(request.getParams().getCacheDirName()).put(entity);
        }
    }

    public abstract Loader<T> newInstance();

    public abstract T load(final InputStream in) throws Throwable;

    public abstract T load(final UriRequest request) throws Throwable;

    public abstract T loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable;

    public abstract void save2Cache(final UriRequest request);
}
