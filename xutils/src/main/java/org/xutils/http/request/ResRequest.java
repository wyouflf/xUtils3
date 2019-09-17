package org.xutils.http.request;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.LruDiskCache;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.http.RequestParams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by wyouflf on 15/11/4.
 * 本地资源请求
 */
public class ResRequest extends UriRequest {

    private static long lastModifiedTime = 0;
    protected long contentLength = 0;
    protected InputStream inputStream;

    public ResRequest(RequestParams params, Type loadType) throws Throwable {
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
            if (lastModifiedDate == null || lastModifiedDate.getTime() < getLastModified()) {
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

    private int getResId() {
        int resId = 0;
        String resIdStr = queryUrl.substring("res:".length());
        resIdStr = resIdStr.replace("/", "");
        if (TextUtils.isDigitsOnly(resIdStr)) {
            resId = Integer.parseInt(resIdStr);
        }

        if (resId <= 0) {
            throw new IllegalArgumentException("resId not found in url:" + queryUrl);
        }

        return resId;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            Context context = params.getContext();
            inputStream = context.getResources().openRawResource(getResId());
            contentLength = inputStream.available();
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
        return -1;
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
        if (lastModifiedTime == 0) {
            try {
                Context context = params.getContext();
                ApplicationInfo appInfo = context.getApplicationInfo();
                File appFile = new File(appInfo.sourceDir);
                if (appFile.exists()) {
                    lastModifiedTime = appFile.lastModified();
                }
            } catch (Throwable ex) {
                LogUtil.w(ex.getMessage(), ex);
                lastModifiedTime = 0;
            } finally {
                if (lastModifiedTime == 0) {
                    lastModifiedTime = System.currentTimeMillis();
                }
            }
        }
        return lastModifiedTime;
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
}
