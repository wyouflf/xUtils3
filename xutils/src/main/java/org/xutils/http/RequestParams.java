package org.xutils.http;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.task.Priority;
import org.xutils.common.util.KeyValue;
import org.xutils.common.util.LogUtil;
import org.xutils.http.annotation.HttpRequest;
import org.xutils.http.app.DefaultParamsBuilder;
import org.xutils.http.app.HttpRetryHandler;
import org.xutils.http.app.ParamsBuilder;
import org.xutils.http.app.RedirectHandler;
import org.xutils.http.body.BodyItemWrapper;
import org.xutils.http.body.FileBody;
import org.xutils.http.body.InputStreamBody;
import org.xutils.http.body.MultipartBody;
import org.xutils.http.body.RequestBody;
import org.xutils.http.body.StringBody;
import org.xutils.http.body.UrlEncodedParamsBody;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLSocketFactory;

/**
 * Created by wyouflf on 15/7/17.
 * 网络请求参数实体
 */
public class RequestParams {

    // 注解及其扩展参数
    private HttpRequest httpRequest;
    private final String uri;
    private final String[] signs;
    private final String[] cacheKeys;
    private ParamsBuilder builder;
    private String buildUri;
    private String buildCacheKey;
    private SSLSocketFactory sslSocketFactory;

    // 请求体内容
    private HttpMethod method;
    private String bodyContent;
    private RequestBody requestBody;
    private final List<Header> headers = new ArrayList<Header>();
    private final List<KeyValue> queryStringParams = new ArrayList<KeyValue>();
    private final List<KeyValue> bodyParams = new ArrayList<KeyValue>();
    private final List<KeyValue> fileParams = new ArrayList<KeyValue>();

    // 扩展参数
    private Proxy proxy; // 代理
    private String charset = "UTF-8";
    private boolean useCookie = true; // 是否在请求过程中启用cookie
    private String cacheDirName; // 缓存文件夹名称
    private long cacheSize; // 缓存文件夹大小
    private long cacheMaxAge; // 默认缓存存活时间, 单位:毫秒.(如果服务没有返回有效的max-age或Expires)
    private boolean asJsonContent = false; // 用json形式的bodyParams上传
    private Executor executor; // 自定义线程池
    private Priority priority = Priority.DEFAULT; // 请求优先级
    private int connectTimeout = 1000 * 15; // 连接超时时间
    private boolean autoResume = true; // 是否在下载是自动断点续传
    private boolean autoRename = false; // 是否根据头信息自动命名文件
    private int maxRetryCount = 2; // 最大请求错误重试次数
    private String saveFilePath; // 下载文件时文件保存的路径和文件名
    private boolean multipart = false; // 是否强制使用multipart表单
    private boolean cancelFast = false; // 是否可以被立即停止, true: 为请求创建新的线程, 取消时请求线程被立即中断.
    private HttpRetryHandler httpRetryHandler; // 自定义HttpRetryHandler
    private RedirectHandler redirectHandler; // 自定义重定向接口, 默认系统自动重定向.

    /**
     * 使用空构造创建时必须, 必须是带有@HttpRequest注解的子类.
     */
    public RequestParams() {
        this(null, null, null, null);
    }

    /**
     * @param uri 不可为空
     */
    public RequestParams(String uri) {
        this(uri, null, null, null);
    }

    /**
     * @param uri       不可为空
     * @param builder
     * @param signs
     * @param cacheKeys
     */
    public RequestParams(String uri, ParamsBuilder builder, String[] signs, String[] cacheKeys) {
        if (uri != null && builder == null) {
            builder = new DefaultParamsBuilder();
        }
        this.uri = uri;
        this.signs = signs;
        this.cacheKeys = cacheKeys;
        this.builder = builder;
    }

    // invoke via UriRequest#<init>()
    /*package*/ void init() throws Throwable {
        if (TextUtils.isEmpty(uri) && getHttpRequest() == null) {
            throw new IllegalStateException("uri is empty && @HttpRequest == null");
        }

        // init params from entity
        initEntityParams();

        // build uri & cacheKey
        buildUri = uri;
        HttpRequest httpRequest = this.getHttpRequest();
        if (httpRequest != null) {
            builder = httpRequest.builder().newInstance();
            buildUri = builder.buildUri(httpRequest);
            builder.buildParams(this);
            builder.buildSign(this, httpRequest.signs());
            if (sslSocketFactory == null) {
                sslSocketFactory = builder.getSSLSocketFactory();
            }
        } else if (this.builder != null) {
            builder.buildParams(this);
            builder.buildSign(this, signs);
            if (sslSocketFactory == null) {
                sslSocketFactory = builder.getSSLSocketFactory();
            }
        }
    }

    public String getUri() {
        return TextUtils.isEmpty(buildUri) ? uri : buildUri;
    }

    public String getCacheKey() {
        if (TextUtils.isEmpty(buildCacheKey) && builder != null) {
            HttpRequest httpRequest = this.getHttpRequest();
            if (httpRequest != null) {
                buildCacheKey = builder.buildCacheKey(this, httpRequest.cacheKeys());
            } else {
                buildCacheKey = builder.buildCacheKey(this, cacheKeys);
            }
        }
        return buildCacheKey;
    }

    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setCharset(String charset) {
        if (!TextUtils.isEmpty(charset)) {
            this.charset = charset;
        }
    }

    public String getCharset() {
        return charset;
    }

    /**
     * 是否在请求过程中启用cookie, 默认true.
     *
     * @return
     */
    public boolean isUseCookie() {
        return useCookie;
    }

    /**
     * 是否在请求过程中启用cookie, 默认true.
     *
     * @param useCookie
     */
    public void setUseCookie(boolean useCookie) {
        this.useCookie = useCookie;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout > 0) {
            this.connectTimeout = connectTimeout;
        }
    }

    public String getCacheDirName() {
        return cacheDirName;
    }

    public void setCacheDirName(String cacheDirName) {
        this.cacheDirName = cacheDirName;
    }

    public long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    /**
     * 默认缓存存活时间, 单位:毫秒.(如果服务没有返回有效的max-age或Expires)
     *
     * @return
     */
    public long getCacheMaxAge() {
        return cacheMaxAge;
    }

    /**
     * 默认缓存存活时间, 单位:毫秒.(如果服务没有返回有效的max-age或Expires)
     *
     * @param cacheMaxAge
     */
    public void setCacheMaxAge(long cacheMaxAge) {
        this.cacheMaxAge = cacheMaxAge;
    }

    /**
     * 自定义线程池
     *
     * @return
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * 自定义线程池
     *
     * @param executor
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * 是否在下载是自动断点续传
     */
    public boolean isAutoResume() {
        return autoResume;
    }

    /**
     * 设置是否在下载是自动断点续传
     *
     * @param autoResume
     */
    public void setAutoResume(boolean autoResume) {
        this.autoResume = autoResume;
    }

    /**
     * 是否根据头信息自动命名文件
     */
    public boolean isAutoRename() {
        return autoRename;
    }

    /**
     * 设置是否根据头信息自动命名文件
     *
     * @param autoRename
     */
    public void setAutoRename(boolean autoRename) {
        this.autoRename = autoRename;
    }

    /**
     * 获取下载文件时文件保存的路径和文件名
     */
    public String getSaveFilePath() {
        return saveFilePath;
    }

    /**
     * 设置下载文件时文件保存的路径和文件名
     *
     * @param saveFilePath
     */
    public void setSaveFilePath(String saveFilePath) {
        this.saveFilePath = saveFilePath;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public void setMultipart(boolean multipart) {
        this.multipart = multipart;
    }

    /**
     * 是否可以被立即停止.
     *
     * @return true: 为请求创建新的线程, 取消时请求线程被立即中断; false: 请求建立过程可能不被立即终止.
     */
    public boolean isCancelFast() {
        return cancelFast;
    }

    /**
     * 是否可以被立即停止.
     *
     * @param cancelFast true: 为请求创建新的线程, 取消时请求线程被立即中断; false: 请求建立过程可能不被立即终止.
     */
    public void setCancelFast(boolean cancelFast) {
        this.cancelFast = cancelFast;
    }

    public HttpRetryHandler getHttpRetryHandler() {
        return httpRetryHandler;
    }

    public void setHttpRetryHandler(HttpRetryHandler httpRetryHandler) {
        this.httpRetryHandler = httpRetryHandler;
    }

    public RedirectHandler getRedirectHandler() {
        return redirectHandler;
    }

    /**
     * 自定义重定向接口, 默认系统自动重定向.
     *
     * @param redirectHandler
     */
    public void setRedirectHandler(RedirectHandler redirectHandler) {
        this.redirectHandler = redirectHandler;
    }

    /**
     * 覆盖header
     *
     * @param name
     * @param value
     */
    public void setHeader(String name, String value) {
        Header header = new Header(name, value, true);
        this.headers.remove(header);
        this.headers.add(header);
    }

    /**
     * 添加header
     *
     * @param name
     * @param value
     */
    public void addHeader(String name, String value) {
        this.headers.add(new Header(name, value, false));
    }

    /**
     * 添加请求参数(根据请求谓词, 将参数加入QueryString或Body.)
     *
     * @param name  参数名
     * @param value 可以是String, File, InputStream 或 byte[]
     */
    public void addParameter(String name, Object value) {
        if (value == null) return;

        if (method == null || HttpMethod.permitsRequestBody(method)) {
            if (!TextUtils.isEmpty(name)) {
                if (value instanceof File
                        || value instanceof InputStream
                        || value instanceof byte[]) {
                    this.fileParams.add(new KeyValue(name, value));
                } else {
                    if (value instanceof List) {
                        for (Object item : (List) value) {
                            this.bodyParams.add(new ArrayItem(name, item));
                        }
                    } else if (value.getClass().isArray()) {
                        int len = Array.getLength(value);
                        for (int i = 0; i < len; i++) {
                            this.bodyParams.add(new ArrayItem(name, Array.get(value, i)));
                        }
                    } else {
                        this.bodyParams.add(new KeyValue(name, value));
                    }
                }
            } else {
                this.bodyContent = value.toString();
            }
        } else {
            if (!TextUtils.isEmpty(name)) {
                if (value instanceof List) {
                    for (Object item : (List) value) {
                        this.queryStringParams.add(new ArrayItem(name, item));
                    }
                } else if (value.getClass().isArray()) {
                    int len = Array.getLength(value);
                    for (int i = 0; i < len; i++) {
                        this.queryStringParams.add(new ArrayItem(name, Array.get(value, i)));
                    }
                } else {
                    this.queryStringParams.add(new KeyValue(name, value));
                }
            }
        }
    }

    /**
     * 添加参数至Query String
     *
     * @param name
     * @param value
     */
    public void addQueryStringParameter(String name, String value) {
        if (!TextUtils.isEmpty(name)) {
            this.queryStringParams.add(new KeyValue(name, value));
        }
    }

    /**
     * 添加参数至Body
     *
     * @param name
     * @param value
     */
    public void addBodyParameter(String name, String value) {
        if (!TextUtils.isEmpty(name)) {
            this.bodyParams.add(new KeyValue(name, value));
        } else {
            this.bodyContent = value;
        }
    }

    /**
     * 添加body参数
     */
    public void addBodyParameter(String name, File value) {
        addBodyParameter(name, value, null, null);
    }

    /**
     * 添加body参数
     *
     * @param name        参数名
     * @param value       可以是String, File, InputStream 或 byte[]
     * @param contentType 可为null
     */
    public void addBodyParameter(String name, Object value, String contentType) {
        addBodyParameter(name, value, contentType, null);
    }

    /**
     * 添加body参数
     *
     * @param name        参数名
     * @param value       可以是String, File, InputStream 或 byte[]
     * @param contentType 可为null
     * @param fileName    服务端看到的文件名
     */
    public void addBodyParameter(String name, Object value, String contentType, String fileName) {
        if (TextUtils.isEmpty(contentType) && TextUtils.isEmpty(fileName)) {
            this.fileParams.add(new KeyValue(name, value));
        } else {
            this.fileParams.add(new KeyValue(name, new BodyItemWrapper(value, contentType, fileName)));
        }
    }

    /**
     * 用json形式的bodyContent上传
     *
     * @return
     */
    public boolean isAsJsonContent() {
        return asJsonContent;
    }

    /**
     * 以json形式提交body参数
     *
     * @param asJsonContent
     */
    public void setAsJsonContent(boolean asJsonContent) {
        this.asJsonContent = asJsonContent;
    }

    public void setBodyContent(String content) {
        this.bodyContent = content;
    }

    public String getBodyContent() {
        checkBodyParams();
        return bodyContent;
    }

    public List<Header> getHeaders() {
        return new ArrayList<Header>(headers);
    }

    public List<KeyValue> getQueryStringParams() {
        checkBodyParams();
        return new ArrayList<KeyValue>(queryStringParams);
    }

    public List<KeyValue> getBodyParams() {
        checkBodyParams();
        return new ArrayList<KeyValue>(bodyParams);
    }

    public List<KeyValue> getFileParams() {
        checkBodyParams();
        return new ArrayList<KeyValue>(fileParams);
    }

    public List<KeyValue> getStringParams() {
        List<KeyValue> result = new ArrayList<KeyValue>(
                queryStringParams.size() + bodyParams.size());
        result.addAll(queryStringParams);
        result.addAll(bodyParams);
        return result;
    }

    public String getStringParameter(String name) {
        for (KeyValue kv : queryStringParams) {
            if (name == null && kv.key == null) {
                return kv.getValueStr();
            } else if (name != null && name.equals(kv.key)) {
                return kv.getValueStr();
            }
        }
        for (KeyValue kv : bodyParams) {
            if (name == null && kv.key == null) {
                return kv.getValueStr();
            } else if (name != null && name.equals(kv.key)) {
                return kv.getValueStr();
            }
        }
        return null;
    }

    public List<KeyValue> getParams(String name) {
        List<KeyValue> result = new ArrayList<KeyValue>();
        for (KeyValue kv : queryStringParams) {
            if (name == null && kv.key == null) {
                result.add(kv);
            } else if (name != null && name.equals(kv.key)) {
                result.add(kv);
            }
        }
        for (KeyValue kv : bodyParams) {
            if (name == null && kv.key == null) {
                result.add(kv);
            } else if (name != null && name.equals(kv.key)) {
                result.add(kv);
            }
        }
        for (KeyValue kv : fileParams) {
            if (name == null && kv.key == null) {
                result.add(kv);
            } else if (name != null && name.equals(kv.key)) {
                result.add(kv);
            }
        }
        return result;
    }

    public void clearParams() {
        queryStringParams.clear();
        bodyParams.clear();
        fileParams.clear();
        bodyContent = null;
        requestBody = null;
    }

    public void removeParameter(String name) {
        if (!TextUtils.isEmpty(name)) {
            Iterator<KeyValue> it = queryStringParams.iterator();
            while (it.hasNext()) {
                KeyValue bkv = it.next();
                if (name.equals(bkv.key)) {
                    it.remove();
                }
            }

            it = bodyParams.iterator();
            while (it.hasNext()) {
                KeyValue bkv = it.next();
                if (name.equals(bkv.key)) {
                    it.remove();
                }
            }

            it = fileParams.iterator();
            while (it.hasNext()) {
                KeyValue bkv = it.next();
                if (name.equals(bkv.key)) {
                    it.remove();
                }
            }
        } else {
            bodyContent = null;
        }
    }

    public void setRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
    }

    public RequestBody getRequestBody() throws IOException {
        checkBodyParams();
        if (this.requestBody != null) {
            return this.requestBody;
        }
        RequestBody result = null;
        if (!TextUtils.isEmpty(bodyContent)) {
            result = new StringBody(bodyContent, charset);
        } else if (multipart || fileParams.size() > 0) {
            if (!multipart && fileParams.size() == 1) {
                for (KeyValue kv : fileParams) {
                    String contentType = null;
                    Object value = kv.value;
                    if (value instanceof BodyItemWrapper) {
                        BodyItemWrapper wrapper = (BodyItemWrapper) value;
                        value = wrapper.getValue();
                        contentType = wrapper.getContentType();
                    }
                    if (value instanceof File) {
                        result = new FileBody((File) value, contentType);
                    } else if (value instanceof InputStream) {
                        result = new InputStreamBody((InputStream) value, contentType);
                    } else if (value instanceof byte[]) {
                        result = new InputStreamBody(new ByteArrayInputStream((byte[]) value), contentType);
                    } else if (value instanceof String) {
                        // invoke addBodyParameter(key, stringValue, contentType)
                        result = new StringBody((String) value, charset);
                        result.setContentType(contentType);
                    } else {
                        LogUtil.w("Some params will be ignored for: " + this.getUri());
                    }
                    break;
                }
            } else {
                multipart = true;
                result = new MultipartBody(fileParams, charset);
            }
        } else if (bodyParams != null && bodyParams.size() > 0) {
            result = new UrlEncodedParamsBody(bodyParams, charset);
        }

        return result;
    }

    private void initEntityParams() {
        addEntityParams2Map(this.getClass());
    }

    private void addEntityParams2Map(Class<?> type) {
        if (type == null || type == RequestParams.class || type == Object.class) {
            return;
        }

        Field[] fields = type.getDeclaredFields();
        if (fields != null && fields.length > 0) {
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    String name = field.getName();
                    Object value = field.get(this);
                    if (value != null) {
                        this.addParameter(name, value);
                    }
                } catch (IllegalAccessException ex) {
                    LogUtil.e(ex.getMessage(), ex);
                }
            }
        }

        addEntityParams2Map(type.getSuperclass());
    }

    private boolean invokedGetHttpRequest = false;

    private HttpRequest getHttpRequest() {
        if (httpRequest == null && !invokedGetHttpRequest) {
            invokedGetHttpRequest = true;
            Class<?> thisCls = this.getClass();
            if (thisCls != RequestParams.class) {
                httpRequest = thisCls.getAnnotation(HttpRequest.class);
            }
        }

        return httpRequest;
    }

    private void checkBodyParams() {
        if (!bodyParams.isEmpty() &&
                (!HttpMethod.permitsRequestBody(method)
                        || !TextUtils.isEmpty(bodyContent)
                        || requestBody != null)) {
            queryStringParams.addAll(bodyParams);
            bodyParams.clear();
        }

        if (!bodyParams.isEmpty() && (multipart || fileParams.size() > 0)) {
            fileParams.addAll(bodyParams);
            bodyParams.clear();
        }

        if (asJsonContent && !bodyParams.isEmpty()) {
            try {
                JSONObject jsonObject = null;
                if (!TextUtils.isEmpty(bodyContent)) {
                    jsonObject = new JSONObject(bodyContent);
                } else {
                    jsonObject = new JSONObject();
                }
                params2Json(jsonObject, bodyParams);
                bodyContent = jsonObject.toString();
                bodyParams.clear();
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void params2Json(final JSONObject jsonObject, final List<KeyValue> paramList) throws JSONException {
        HashSet<String> arraySet = new HashSet<String>(paramList.size());
        LinkedHashMap<String, JSONArray> tempData = new LinkedHashMap<String, JSONArray>(paramList.size());
        for (int i = 0; i < paramList.size(); i++) {
            KeyValue kv = paramList.get(i);
            final String key = kv.key;
            if (TextUtils.isEmpty(key)) continue;

            JSONArray ja = null;
            if (tempData.containsKey(key)) {
                ja = tempData.get(key);
            } else {
                ja = new JSONArray();
                tempData.put(key, ja);
            }
            ja.put(kv.value);

            if (kv instanceof ArrayItem) {
                arraySet.add(key);
            }
        }

        for (Map.Entry<String, JSONArray> entry : tempData.entrySet()) {
            String key = entry.getKey();
            JSONArray ja = entry.getValue();
            if (ja.length() > 1 || arraySet.contains(key)) {
                jsonObject.put(key, ja);
            } else {
                Object value = ja.get(0);
                jsonObject.put(key, value);
            }
        }
    }

    @Override
    public String toString() {
        return getUri();
    }

    public final static class ArrayItem extends KeyValue {
        public ArrayItem(String key, Object value) {
            super(key, value);
        }
    }

    public final static class Header extends KeyValue {
        public final boolean setHeader;

        public Header(String key, String value, boolean setHeader) {
            super(key, value);
            this.setHeader = setHeader;
        }
    }
}
