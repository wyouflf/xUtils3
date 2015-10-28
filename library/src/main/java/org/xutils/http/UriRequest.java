package org.xutils.http;

import android.net.Uri;
import android.text.TextUtils;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.HttpDate;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.LruDiskCache;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.HttpException;
import org.xutils.http.app.ResponseTracker;
import org.xutils.http.body.ProgressBody;
import org.xutils.http.cookie.DbCookieStore;
import org.xutils.http.loader.Loader;
import org.xutils.http.loader.LoaderFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

/**
 * Created by wyouflf on 15/7/23.
 * Uri请求发送和数据接收
 */
public final class UriRequest implements Closeable {

    private final String uri;
    private final RequestParams params;
    private final Loader<?> loader;

    // cookie manager
    private static final CookieManager COOKIE_MANAGER =
            new CookieManager(DbCookieStore.INSTANCE, CookiePolicy.ACCEPT_ALL);

    private ProgressCallbackHandler progressHandler;

    private Call call;
    private Request request = null;
    private String cacheKey = null;

    private boolean isLoading = false;
    private Response response = null;

    private ClassLoader callingClassLoader;
    private InputStream inputStream = null;


    public UriRequest(RequestParams params, Class<?> loadType) throws Throwable {
        params.init();
        this.params = params;
        this.uri = buildUri(params);
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

    public void setProgressCallbackHandler(ProgressCallbackHandler progressHandler) {
        this.progressHandler = progressHandler;
        this.loader.setProgressCallbackHandler(progressHandler);
    }

    public void setCallingClassLoader(ClassLoader callingClassLoader) {
        this.callingClassLoader = callingClassLoader;
    }

    public String getRequestUri() {
        return request == null ? uri : request.urlString();
    }

    /**
     * invoke via Loader
     *
     * @throws IOException
     */
    public void sendRequest() throws IOException {
        isLoading = false;
        if (uri.startsWith("http")) {

            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();

            {// create builder
                // set client params
                client.setProxy(params.getProxy());
                client.setConnectTimeout(params.getConnectTimeout(), TimeUnit.MILLISECONDS);
                client.setReadTimeout(params.getConnectTimeout(), TimeUnit.MILLISECONDS);
                client.setWriteTimeout(params.getConnectTimeout(), TimeUnit.MILLISECONDS);
                client.setCookieHandler(COOKIE_MANAGER);
                SSLSocketFactory sslSocketFactory = params.getSslSocketFactory();
                if (sslSocketFactory != null) {
                    client.setSslSocketFactory(sslSocketFactory);
                }

                builder.url(this.uri);

                // add headers
                HashMap<String, String> headers = params.getHeaders();
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        String name = entry.getKey();
                        String value = entry.getValue();
                        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                            builder.addHeader(name, value);
                        }
                    }
                }

                // add body
                RequestBody body = params.getRequestBody();
                if (body instanceof ProgressBody) {
                    ((ProgressBody) body).setProgressCallbackHandler(progressHandler);
                }
                builder.method(params.getMethod().toString(), body);
            } // create builder


            request = builder.build();
            LogUtil.d(request.urlString());
            call = client.newCall(request);
            response = call.execute();

            // 3xx重定向?
            int code = response.code();
            if (code >= 300) {
                throw new HttpException(code, response.message());
            }

        }
        isLoading = true;
    }

    public ResponseTracker getResponseTracker() {
        return loader.getResponseTracker();
    }

    /**
     * 由loader发起请求, 拿到结果.
     *
     * @return
     * @throws Throwable
     */
    public Object loadResult() throws Throwable {
        return loader.load(this);
    }

    /**
     * 尝试从缓存获取结果, 并为请求头加入缓存控制参数.
     *
     * @return
     * @throws Throwable
     */
    public Object loadResultFromCache() throws Throwable {
        DiskCacheEntity cacheEntity = LruDiskCache.getDiskCache(params.getCacheDirName()).get(this.getCacheKey());

        if (cacheEntity != null) {
            Date lastModified = cacheEntity.getLastModify();
            if (lastModified.getTime() > 0) {
                params.addHeader("If-Modified-Since", toGMTString(lastModified));
            }
            String eTag = cacheEntity.getEtag();
            if (!TextUtils.isEmpty(eTag)) {
                params.addHeader("If-None-Match", eTag);
            }

            return loader.loadFromCache(cacheEntity);
        } else {
            return null;
        }
    }

    public void clearCacheHeader() {
        params.addHeader("If-Modified-Since", null);
        params.addHeader("If-None-Match", null);
    }

    public void save2Cache() {
        loader.save2Cache(this);
    }

    public boolean isLoading() {
        return isLoading;
    }

    public String getCacheKey() {
        if (cacheKey == null) {

            cacheKey = params.getCacheKey();

            if (cacheKey == null) {
                cacheKey = uri;
            }
        }
        return cacheKey;
    }

    @Override
    public void close() throws IOException {
        if (response != null) {
            IOUtil.closeQuietly(response.body());
            response = null;
        }
        if (inputStream != null) {
            IOUtil.closeQuietly(inputStream);
            inputStream = null;
        }
        if (call != null && !call.isCanceled()) {
            call.cancel();
            call = null;
        }
    }

    public RequestParams getParams() {
        return params;
    }

    public Response getResponse() throws IOException {
        return response;
    }

    public File getFile() {
        if (uri.startsWith("file://")) {
            String filePath = uri.substring(7);
            return new File(filePath);
        } else {
            try {
                File file = new File(uri);
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
            if (response != null) {
                inputStream = response.body().byteStream();
            } else {
                if (callingClassLoader != null && uri.startsWith("assets://")) {
                    inputStream = callingClassLoader.getResourceAsStream(uri);
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

    public long getContentLength() {
        long result = 0;
        if (response != null) {
            try {
                result = response.body().contentLength();
            } catch (IOException ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
            if (result < 1) {
                try {
                    result = this.getInputStream().available();
                } catch (IOException ignored) {
                }
            }
        } else {
            try {
                result = this.getInputStream().available();
            } catch (IOException ignored) {
            }
        }
        return result;
    }

    public int getResponseCode() throws IOException {
        if (response != null) {
            return response.code();
        } else {
            if (this.getInputStream() != null) {
                return 200;
            } else {
                return 404;
            }
        }
    }

    public long getExpiration() {
        if (response == null) return -1;
        long result = -1;
        int maxSeconds = response.cacheControl().maxAgeSeconds();
        if (maxSeconds <= 0) {
            result = Long.MAX_VALUE;
        } else {
            result = System.currentTimeMillis() + maxSeconds * 1000L;
        }
        return result;
    }

    public long getLastModified() {
        return getHeaderFieldDate("Last-Modified", System.currentTimeMillis());
    }

    public String getETag() {
        if (response == null) return null;
        return response.header("ETag");
    }

    public String getResponseHeader(String name) {
        if (response == null) return null;
        return response.header(name);
    }

    public Headers getResponseHeaders() {
        if (response == null) return null;
        return response.headers();
    }

    public List<String> getResponseHeaders(String name) {
        if (response == null) return null;
        return response.headers(name);
    }

    private long getHeaderFieldDate(String name, long defaultValue) {
        if (response == null) return defaultValue;
        String date = response.header(name);
        if (date == null) {
            return defaultValue;
        }
        try {
            return HttpDate.parse(date).getTime();
        } catch (Exception ignored) {
            return defaultValue;
        }
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
