package org.xutils.http.request;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.LruDiskCache;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by wyouflf on 15/11/4.
 * Assets资源文件请求
 */
public class AssetsRequest extends UriRequest {

    private long contentLength = 0;
    private InputStream inputStream;

    public AssetsRequest(RequestParams params, Type loadType) throws Throwable {
        super(params, loadType);
    }

    @Override
    public void sendRequest() throws Throwable {

    }

    @Override
    public boolean isLoading() {
        return true;
    }

    @Override
    public String getCacheKey() {
        return queryUrl;
    }

    @Override
    public Object loadResult() throws Throwable {
        return this.loader.load(this);
    }

    @Override
    public Object loadResultFromCache() throws Throwable {
        DiskCacheEntity cacheEntity = LruDiskCache.getDiskCache(params.getCacheDirName())
                .setMaxSize(params.getCacheSize())
                .get(this.getCacheKey());

        if (cacheEntity != null) {
            Date lastModifiedDate = cacheEntity.getLastModify();
            if (lastModifiedDate == null || lastModifiedDate.getTime() < getAssetsLastModified()) {
                return null;
            }
            return loader.loadFromCache(cacheEntity);
        } else {
            return null;
        }
    }

    @Override
    public void clearCacheHeader() {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            if (callingClassLoader != null) {
                String assetsPath = "assets/" + queryUrl.substring("assets://".length());
                inputStream = callingClassLoader.getResourceAsStream(assetsPath);
                contentLength = inputStream.available();
            }
        }
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        IOUtil.closeQuietly(inputStream);
        inputStream = null;
    }

    @Override
    public long getContentLength() {
        try {
            getInputStream();
            return contentLength;
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
        return 0;
    }

    @Override
    public int getResponseCode() throws IOException {
        return getInputStream() != null ? 200 : 404;
    }

    @Override
    public String getResponseMessage() throws IOException {
        return null;
    }

    @Override
    public long getExpiration() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getLastModified() {
        return getAssetsLastModified();
    }

    @Override
    public String getETag() {
        return null;
    }

    @Override
    public String getResponseHeader(String name) {
        return null;
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return null;
    }

    @Override
    public long getHeaderFieldDate(String name, long defaultValue) {
        return defaultValue;
    }

    /**
     * 如果你的应用基于插件架构, 并且插件有独立的资源管理实现, 可能需要覆盖这里的实现方式,
     * 并在UriRequestFactory中注册你的实现.
     */
    protected long getAssetsLastModified() {
        return new File(x.app().getApplicationInfo().sourceDir).lastModified();
    }

}
