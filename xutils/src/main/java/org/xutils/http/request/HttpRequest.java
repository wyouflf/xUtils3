package org.xutils.http.request;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.LruDiskCache;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.KeyValue;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.HttpException;
import org.xutils.http.HttpMethod;
import org.xutils.http.RequestParams;
import org.xutils.http.body.ProgressBody;
import org.xutils.http.body.RequestBody;
import org.xutils.http.cookie.DbCookieStore;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by wyouflf on 15/7/23.
 * Uri请求发送和数据接收
 */
public class HttpRequest extends UriRequest {

    private String cacheKey = null;
    private boolean isLoading = false;
    private InputStream inputStream = null;
    private HttpURLConnection connection = null;
    private int responseCode = 0;

    // cookie manager
    private static final CookieManager COOKIE_MANAGER =
            new CookieManager(DbCookieStore.INSTANCE, CookiePolicy.ACCEPT_ALL);

    /*package*/ HttpRequest(RequestParams params, Type loadType) throws Throwable {
        super(params, loadType);
    }

    // build query
    @Override
    protected String buildQueryUrl(RequestParams params) {
        String uri = params.getUri();
        StringBuilder queryBuilder = new StringBuilder(uri);
        if (!uri.contains("?")) {
            queryBuilder.append("?");
        } else if (!uri.endsWith("?")) {
            queryBuilder.append("&");
        }
        List<KeyValue> queryParams = params.getQueryStringParams();
        if (queryParams != null) {
            for (KeyValue kv : queryParams) {
                String name = kv.key;
                String value = kv.getValueStr();
                if (!TextUtils.isEmpty(name) && value != null) {
                    queryBuilder.append(
                            Uri.encode(name, params.getCharset()))
                            .append("=")
                            .append(Uri.encode(value, params.getCharset()))
                            .append("&");
                }
            }
        }

        if (queryBuilder.charAt(queryBuilder.length() - 1) == '&') {
            queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        }

        if (queryBuilder.charAt(queryBuilder.length() - 1) == '?') {
            queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        }
        return queryBuilder.toString();
    }

    @Override
    public String getRequestUri() {
        String result = queryUrl;
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
    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void sendRequest() throws Throwable {
        isLoading = false;
        responseCode = 0;

        URL url = new URL(queryUrl);
        { // init connection
            Proxy proxy = params.getProxy();
            if (proxy != null) {
                connection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            // try to fix bug: accidental EOFException before API 19
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                connection.setRequestProperty("Connection", "close");
            }

            connection.setReadTimeout(params.getConnectTimeout());
            connection.setConnectTimeout(params.getConnectTimeout());
            connection.setInstanceFollowRedirects(params.getRedirectHandler() == null);
            if (connection instanceof HttpsURLConnection) {
                SSLSocketFactory sslSocketFactory = params.getSslSocketFactory();
                if (sslSocketFactory != null) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
                }
            }
        }

        if (params.isUseCookie()) {// add cookies
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
        }

        {// add headers
            List<RequestParams.Header> headers = params.getHeaders();
            if (headers != null) {
                for (RequestParams.Header header : headers) {
                    String name = header.key;
                    String value = header.getValueStr();
                    if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                        if (header.setHeader) {
                            connection.setRequestProperty(name, value);
                        } else {
                            connection.addRequestProperty(name, value);
                        }
                    }
                }
            }
        }

        // intercept response
        if (requestInterceptListener != null) {
            requestInterceptListener.beforeRequest(this);
        }

        { // write body
            HttpMethod method = params.getMethod();
            try {
                connection.setRequestMethod(method.toString());
            } catch (ProtocolException ex) {
                try { // fix: HttpURLConnection not support PATCH method.
                    Field methodField = HttpURLConnection.class.getDeclaredField("method");
                    methodField.setAccessible(true);
                    methodField.set(connection, method.toString());
                } catch (Throwable ignored) {
                    throw ex;
                }
            }
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
                    long contentLength = body.getContentLength();
                    if (contentLength < 0) {
                        connection.setChunkedStreamingMode(256 * 1024);
                    } else {
                        if (contentLength < Integer.MAX_VALUE) {
                            connection.setFixedLengthStreamingMode((int) contentLength);
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            connection.setFixedLengthStreamingMode(contentLength);
                        } else {
                            connection.setChunkedStreamingMode(256 * 1024);
                        }
                    }
                    connection.setRequestProperty("Content-Length", String.valueOf(contentLength));
                    connection.setDoOutput(true);
                    body.writeTo(connection.getOutputStream());
                }
            }
        }

        if (params.isUseCookie()) { // save cookies
            try {
                Map<String, List<String>> headers = connection.getHeaderFields();
                if (headers != null) {
                    COOKIE_MANAGER.put(url.toURI(), headers);
                }
            } catch (Throwable ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
        }

        // check response code
        responseCode = connection.getResponseCode();
        // intercept response
        if (requestInterceptListener != null) {
            requestInterceptListener.afterRequest(this);
        }
        if (responseCode == 204 || responseCode == 205) { // empty content
            throw new HttpException(responseCode, this.getResponseMessage());
        } else if (responseCode >= 300) {
            HttpException httpException = new HttpException(responseCode, this.getResponseMessage());
            try {
                httpException.setResult(IOUtil.readStr(this.getInputStream(), params.getCharset()));
            } catch (Throwable ignored) {
            }
            LogUtil.e(httpException.toString() + ", url: " + queryUrl);
            throw httpException;
        }

        isLoading = true;
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public String getCacheKey() {
        if (cacheKey == null) {

            cacheKey = params.getCacheKey();

            if (TextUtils.isEmpty(cacheKey)) {
                cacheKey = params.toString();
            }
        }
        return cacheKey;
    }

    @Override
    public Object loadResult() throws Throwable {
        isLoading = true;
        return super.loadResult();
    }

    /**
     * 尝试从缓存获取结果, 并为请求头加入缓存控制参数.
     *
     * @return
     * @throws Throwable
     */
    @Override
    public Object loadResultFromCache() throws Throwable {
        isLoading = true;
        DiskCacheEntity cacheEntity = LruDiskCache.getDiskCache(params.getCacheDirName())
                .setMaxSize(params.getCacheSize())
                .get(this.getCacheKey());

        if (cacheEntity != null) {
            if (HttpMethod.permitsCache(params.getMethod())) {
                Date lastModified = cacheEntity.getLastModify();
                if (lastModified.getTime() > 0) {
                    params.setHeader("If-Modified-Since", toGMTString(lastModified));
                }
                String eTag = cacheEntity.getEtag();
                if (!TextUtils.isEmpty(eTag)) {
                    params.setHeader("If-None-Match", eTag);
                }
            }
            return loader.loadFromCache(cacheEntity);
        } else {
            return null;
        }
    }

    @Override
    public void clearCacheHeader() {
        params.setHeader("If-Modified-Since", null);
        params.setHeader("If-None-Match", null);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (connection != null && inputStream == null) {
            inputStream = connection.getResponseCode() >= 400 ?
                    connection.getErrorStream() : connection.getInputStream();
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
            //connection = null;
        }
    }

    @Override
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

    @Override
    public int getResponseCode() throws IOException {
        if (connection != null) {
            return responseCode;
        } else {
            if (this.getInputStream() != null) {
                return 200;
            } else {
                return 404;
            }
        }
    }

    @Override
    public String getResponseMessage() throws IOException {
        if (connection != null) {
            return URLDecoder.decode(connection.getResponseMessage(), params.getCharset());
        } else {
            return null;
        }
    }

    @Override
    public long getExpiration() {
        if (connection == null) return -1L;

        long expiration = -1L;

        // from max-age
        String cacheControl = connection.getHeaderField("Cache-Control");
        if (!TextUtils.isEmpty(cacheControl)) {
            StringTokenizer tok = new StringTokenizer(cacheControl, ",");
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken().trim().toLowerCase();
                if (token.startsWith("max-age")) {
                    int eqIdx = token.indexOf('=');
                    if (eqIdx > 0) {
                        try {
                            String value = token.substring(eqIdx + 1).trim();
                            long seconds = Long.parseLong(value);
                            if (seconds > 0L) {
                                expiration = System.currentTimeMillis() + seconds * 1000L;
                            }
                        } catch (Throwable ex) {
                            LogUtil.e(ex.getMessage(), ex);
                        }
                    }
                    break;
                }
            }
        }

        // from expires
        if (expiration <= 0L) {
            expiration = connection.getExpiration();
        }

        if (expiration <= 0L && params.getCacheMaxAge() > 0L) {
            expiration = System.currentTimeMillis() + params.getCacheMaxAge();
        }

        if (expiration <= 0L) {
            expiration = Long.MAX_VALUE;
        }
        return expiration;
    }

    @Override
    public long getLastModified() {
        return getHeaderFieldDate("Last-Modified", System.currentTimeMillis());
    }

    @Override
    public String getETag() {
        if (connection == null) return null;
        return connection.getHeaderField("ETag");
    }

    @Override
    public String getResponseHeader(String name) {
        if (connection == null) return null;
        return connection.getHeaderField(name);
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        if (connection == null) return null;
        return connection.getHeaderFields();
    }

    @Override
    public long getHeaderFieldDate(String name, long defaultValue) {
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
}
