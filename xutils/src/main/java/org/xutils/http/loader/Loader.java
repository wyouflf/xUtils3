package org.xutils.http.loader;


import android.text.TextUtils;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.LruDiskCache;
import org.xutils.http.ProgressHandler;
import org.xutils.http.RequestParams;
import org.xutils.http.request.UriRequest;

import java.io.InputStream;
import java.util.Date;

/**
 * 数据加载器(根据本地文件流或服务器数据流中解析成特定实体类)
 * Author: wyouflf
 * Time: 2014/05/26
 */
public abstract class Loader<T> {

    protected RequestParams params;
    protected ProgressHandler progressHandler;

    public void setParams(final RequestParams params) {
        this.params = params;
    }

    public void setProgressHandler(final ProgressHandler callbackHandler) {
        this.progressHandler = callbackHandler;
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

    /**
     * 通过输入流加载
     * @param in 输入流
     * @return 实体对象
     * @throws Throwable
     */
    public abstract T load(final InputStream in) throws Throwable;

    /**
     * 通过Uri请求加载, 一般做些特殊处理后 转成InputStream交给Load(InputStream)处理
     * @param request
     * @return
     * @throws Throwable
     */
    public abstract T load(final UriRequest request) throws Throwable;

    public abstract T loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable;

    public abstract void save2Cache(final UriRequest request);
}
