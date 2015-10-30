package org.xutils.http;

import android.net.Uri;
import android.text.TextUtils;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.LruDiskCache;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.HttpException;
import org.xutils.http.app.RequestTracker;
import org.xutils.http.body.ProgressBody;
import org.xutils.http.body.RequestBody;
import org.xutils.http.cookie.DbCookieStore;
import org.xutils.http.loader.Loader;
import org.xutils.http.loader.LoaderFactory;
import org.xutils.x;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by wyouflf on 15/7/23.
 * Uri请求发送和数据接收
 */
public final class UriRequest implements Closeable {

    private final String buildUri;
    private final RequestParams params;
    private final Loader<?> loader;

    private String cacheKey = null;
    private boolean isLoading = false;
    private ClassLoader callingClassLoader = null;
    private InputStream inputStream = null;
    private HttpURLConnection connection = null;

    private ProgressHandler progressHandler = null;

    // cookie manager
    private static final CookieManager COOKIE_MANAGER =
            new CookieManager(DbCookieStore.INSTANCE, CookiePolicy.ACCEPT_ALL);

    public UriRequest(RequestParams params, Class<?> loadType) throws Throwable {
        params.init();
        this.params = params;
        this.buildUri = buildUri(params);
        this.loader = LoaderFactory.getLoader(loadType, params);
    }

    // build query
    private static String buildUri(RequestParams params) {
        String uri = params.getUri();
        StringBuilder queryBuilder = new StringBuilder(uri);
        if (!uri.contains("?")) {
            queryBuilder.append("?");
        } else if (!uri.endsWith("?")) {
            queryBuilder.append("&");
        }
        HashMap<String, String> queryParams = params.getQueryStringParams();
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                    queryBuilder.append(Uri.encode(name)).append("=").append(Uri.encode(value)).append("&");
                }
            }
            if (queryBuilder.charAt(queryBuilder.length() - 1) == '&') {
                queryBuilder.deleteCharAt(queryBuilder.length() - 1);
            }
        }

        if (queryBuilder.charAt(queryBuilder.length() - 1) == '?') {
            queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        }
        return queryBuilder.toString();
    }

    public void setProgressHandler(ProgressHandler progressHandler) {
        this.progressHandler = progressHandler;
        this.loader.setProgressHandler(progressHandler);
    }

    public void setCallingClassLoader(ClassLoader callingClassLoader) {
        this.callingClassLoader = callingClassLoader;
    }

    public RequestParams getParams() {
        return params;
    }

    public String getRequestUri() {
        String result = buildUri;
        if (connection != null) {
            URL url = connection.getURL();
            if (url != null) {
                result = url.toString();
            }
        }
        return result;
    }

    /**
     * invoke via Loader
     *
     * @throws IOException
     */
    public void sendRequest() throws IOException {
        isLoading = false;
        if (buildUri.startsWith("http")) {

            URL url = new URL(buildUri);
            { // init connection
                Proxy proxy = params.getProxy();
                if (proxy != null) {
                    connection = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    connection = (HttpURLConnection) url.openConnection();
                }
                connection.setInstanceFollowRedirects(true);
                connection.setReadTimeout(params.getConnectTimeout());
                connection.setConnectTimeout(params.getConnectTimeout());
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(params.getSslSocketFactory());
                }
            }

            {// add headers

                try {
                    Map<String, List<String>> singleMap =
                            COOKIE_MANAGER.get(url.toURI(), new HashMap<String, List<String>>(0));
                    List<String> cookies = singleMap.get("Cookie");
                    if (cookies != null) {
                        connection.setRequestProperty("Cookie", TextUtils.join(";", cookies));
                    }
                } catch (Throwable ex) {
                    LogUtil.e(ex.getMessage(), ex);
                }

                HashMap<String, String> headers = params.getHeaders();
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        String name = entry.getKey();
                        String value = entry.getValue();
                        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                            connection.setRequestProperty(name, value);
                        }
                    }
                }

            }

            { // write body
                HttpMethod method = params.getMethod();
                connection.setRequestMethod(method.toString());
                if (HttpMethod.permitsRequestBody(method)) {
                    RequestBody body = params.getRequestBody();
                    if (body != null) {
                        if (body instanceof ProgressBody) {
                            ((ProgressBody) body).setProgressHandler(progressHandler);
                        }
                        String contentType = body.getContentType();
                        if (!TextUtils.isEmpty(contentType)) {
                            connection.setRequestProperty("Content-Type", contentType);
                        }
                        connection.setRequestProperty("Content-Length", String.valueOf(body.getContentLength()));
                        connection.setDoOutput(true);
                        body.writeTo(connection.getOutputStream());
                    }
                }
            }

            LogUtil.d(buildUri);
            int code = connection.getResponseCode();
            if (code >= 300) {
                throw new HttpException(code, connection.getResponseMessage());
            }

            { // save cookies
                try {
                    Map<String, List<String>> headers = connection.getHeaderFields();
                    if (headers != null) {
                        COOKIE_MANAGER.put(url.toURI(), headers);
                    }
                } catch (Throwable ex) {
                    LogUtil.e(ex.getMessage(), ex);
                }
            }

        }
        isLoading = true;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public String getCacheKey() {
        if (cacheKey == null) {

            cacheKey = params.getCacheKey();

            if (cacheKey == null) {
                cacheKey = buildUri;
            }
        }
        return cacheKey;
    }

    /*package*/ RequestTracker getResponseTracker() {
        return loader.getResponseTracker();
    }

    /**
     * 由loader发起请求, 拿到结果.
     *
     * @return
     * @throws Throwable
     */
    /*package*/ Object loadResult() throws Throwable {
        return loader.load(this);
    }

    /**
     * 尝试从缓存获取结果, 并为请求头加入缓存控制参数.
     *
     * @return
     * @throws Throwable
     */
    /*package*/ Object loadResultFromCache() throws Throwable {
        DiskCacheEntity cacheEntity = LruDiskCache.getDiskCache(params.getCacheDirName()).get(this.getCacheKey());

        if (cacheEntity != null) {
            if (HttpMethod.permitsCache(params.getMethod())) {
                Date lastModified = cacheEntity.getLastModify();
                if (lastModified.getTime() > 0) {
                    params.addHeader("If-Modified-Since", toGMTString(lastModified));
                }
                String eTag = cacheEntity.getEtag();
                if (!TextUtils.isEmpty(eTag)) {
                    params.addHeader("If-None-Match", eTag);
                }
            }
            return loader.loadFromCache(cacheEntity);
        } else {
            return null;
        }
    }

    /*package*/ void clearCacheHeader() {
        params.addHeader("If-Modified-Since", null);
        params.addHeader("If-None-Match", null);
    }

    /*package*/ void save2Cache() {
        x.task().run(new Runnable() {
            @Override
            public void run() {
                try {
                    loader.save2Cache(UriRequest.this);
                } catch (Throwable ex) {
                    LogUtil.e(ex.getMessage(), ex);
                }
            }
        });
    }

    public File getFile() {
        if (buildUri.startsWith("file://")) {
            String filePath = buildUri.substring(7);
            return new File(filePath);
        } else {
            try {
                File file = new File(buildUri);
                if (file.exists()) {
                    return file;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            if (connection != null) {
                inputStream = connection.getInputStream();
            } else {
                if (callingClassLoader != null && buildUri.startsWith("assets://")) {
                    inputStream = callingClassLoader.getResourceAsStream(buildUri);
                } else {
                    File file = getFile();
                    if (file != null && file.exists()) {
                        inputStream = new FileInputStream(file);
                    }
                }
            }
        }
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            IOUtil.closeQuietly(inputStream);
            inputStream = null;
        }
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    public long getContentLength() {
        long result = 0;
        if (connection != null) {
            try {
                result = connection.getContentLength();
            } catch (Throwable ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
            if (result < 1) {
                try {
                    result = this.getInputStream().available();
                } catch (Throwable ignored) {
                }
            }
        } else {
            try {
                result = this.getInputStream().available();
            } catch (Throwable ignored) {
            }
        }
        return result;
    }

    public int getResponseCode() throws IOException {
        if (connection != null) {
            return connection.getResponseCode();
        } else {
            if (this.getInputStream() != null) {
                return 200;
            } else {
                return 404;
            }
        }
    }

    public long getExpiration() {
        if (connection == null) return -1;
        long result = connection.getExpiration();
        if (result <= 0) {
            result = Long.MAX_VALUE;
        }
        return result;
    }

    public long getLastModified() {
        return getHeaderFieldDate("Last-Modified", System.currentTimeMillis());
    }

    public String getETag() {
        if (connection == null) return null;
        return connection.getHeaderField("ETag");
    }

    public String getResponseHeader(String name) {
        if (connection == null) return null;
        return connection.getHeaderField(name);
    }

    public Map<String, List<String>> getResponseHeaders() {
        if (connection == null) return null;
        return connection.getHeaderFields();
    }

    private long getHeaderFieldDate(String name, long defaultValue) {
        if (connection == null) return defaultValue;
        return connection.getHeaderFieldDate(name, defaultValue);
    }

    private static String toGMTString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE, dd MMM y HH:mm:ss 'GMT'", Locale.US);
        TimeZone gmtZone = TimeZone.getTimeZone("GMT");
        sdf.setTimeZone(gmtZone);
        GregorianCalendar gc = new GregorianCalendar(gmtZone);
        gc.setTimeInMillis(date.getTime());
        return sdf.format(date);
    }

    @Override
    public String toString() {
        return getRequestUri();
    }
}
