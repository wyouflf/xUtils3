package org.xutils.http.loader;


import android.text.TextUtils;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.LruDiskCache;
import org.xutils.http.ProgressHandler;
import org.xutils.http.RequestParams;
import org.xutils.http.request.UriRequest;

import java.util.Date;

/**
 * Author: wyouflf
 * Time: 2014/05/26
 */
public abstract class Loader<T> {

    protected ProgressHandler progressHandler;

    public void setParams(final RequestParams params) {
    }

    public void setProgressHandler(final ProgressHandler callbackHandler) {
        this.progressHandler = callbackHandler;
    }

    protected void saveStringCache(UriRequest request, String resultStr) {
        saveCacheInternal(request, resultStr, null);
    }

    protected void saveByteArrayCache(UriRequest request, byte[] resultData) {
        saveCacheInternal(request, null, resultData);
    }

    public abstract Loader<T> newInstance();

    public abstract T load(final UriRequest request) throws Throwable;

    public abstract T loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable;

    public abstract void save2Cache(final UriRequest request);

    private void saveCacheInternal(UriRequest request, String resultStr, byte[] resultData) {
        if (!TextUtils.isEmpty(resultStr) || (resultData != null && resultData.length > 0)) {
            DiskCacheEntity entity = new DiskCacheEntity();
            entity.setKey(request.getCacheKey());
            entity.setLastAccess(System.currentTimeMillis());
            entity.setEtag(request.getETag());
            entity.setExpires(request.getExpiration());
            entity.setLastModify(new Date(request.getLastModified()));
            entity.setTextContent(resultStr);
            entity.setBytesContent(resultData);
            LruDiskCache.getDiskCache(request.getParams().getCacheDirName()).put(entity);
        }
    }
}
