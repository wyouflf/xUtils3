package org.xutils.http;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.task.Priority;
import org.xutils.common.util.LogUtil;
import org.xutils.http.annotation.HttpRequest;
import org.xutils.http.app.DefaultParamsBuilder;
import org.xutils.http.app.ParamsBuilder;
import org.xutils.http.body.BodyParamsBody;
import org.xutils.http.body.ContentTypeWrapper;
import org.xutils.http.body.FileBody;
import org.xutils.http.body.InputStreamBody;
import org.xutils.http.body.MultipartBody;
import org.xutils.http.body.RequestBody;
import org.xutils.http.body.StringBody;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLSocketFactory;

/**
 * Created by wyouflf on 15/7/17.
 * 网络请求参数实体
 */
public class RequestParams {

    // 外部参数
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
    private HashMap<String, String> headers;
    private HashMap<String, String> queryStringParams;
    private HashMap<String, String> bodyParams;
    private HashMap<String, Object> fileParams;
    private RequestBody requestBody;

    // 扩展参数
    private Proxy proxy; // 代理
    private String charset = "UTF-8";
    private String cacheDirName; // 缓存文件夹名称
    private boolean asJsonContent = false; // 用json形式的bodyParams上传
    private HttpRequest httpRequest; // 注解参数
    private Executor executor; // 自定义线程池
    private Priority priority = Priority.DEFAULT; // 请求优先级
    private int connectTimeout = 1000 * 15; // 连接超时时间
    private boolean autoResume = true; // 是否在下载是自动断点续传
    private boolean autoRename = false; // 是否根据头信息自动命名文件
    private int maxRetryCount = 2; // 最大请求错误重试次数
    private String saveFilePath; // 下载文件时文件保存的路径和文件名
    private boolean multipart = false; // 是否强制使用multipart表单
    private boolean cancelFast = false; // 是否可以被立即停止, true: 为请求创建新的线程, 取消时请求线程被立即中断.

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
     * @param builder   不可为空
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

        initEntityParams();

        // build uri & cacheKey
        HttpRequest httpRequest = this.getHttpRequest();
        if (httpRequest != null) {
            builder = httpRequest.builder().newInstance();
            builder.buildParams(this);
            builder.buildSign(this, httpRequest.signs());
            buildUri = builder.buildUri(httpRequest);
            buildCacheKey = builder.buildCacheKey(this, httpRequest.cacheKeys());
            sslSocketFactory = builder.getSSLSocketFactory();
        } else if (this.builder != null) {
            builder.buildParams(this);
            builder.buildSign(this, signs);
            buildUri = uri;
            buildCacheKey = builder.buildCacheKey(this, cacheKeys);
            sslSocketFactory = builder.getSSLSocketFactory();
        }
    }

    public String getUri() {
        return TextUtils.isEmpty(buildUri) ? uri : buildUri;
    }

    public String getCacheKey() {
        return buildCacheKey;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    /*package*/ void setMethod(HttpMethod method) {
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

    public Executor getExecutor() {
        return executor;
    }

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

    public void addHeader(String name, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<String, String>();
        }
        this.headers.put(name, value);
    }

    public void addParameter(String name, Object value) {
        if (value == null) return;
        if (HttpMethod.permitsRequestBody(method)) {
            if (!TextUtils.isEmpty(name)) {
                if (value instanceof String) {
                    this.addBodyParameter(name, (String) value);
                } else {
                    this.addBodyParameter(name, value, null);
                }
            } else if (TextUtils.isEmpty(name)) {
                this.setBodyContent(value.toString());
            }
        } else {
            if (!TextUtils.isEmpty(name)) {
                this.addQueryStringParameter(name, value.toString());
            }
        }
    }

    public void addQueryStringParameter(String name, String value) {
        if (this.queryStringParams == null) {
            this.queryStringParams = new HashMap<String, String>();
        }
        this.queryStringParams.put(name, value);
    }

    public void addBodyParameter(String name, String value) {
        if (this.bodyParams == null) {
            this.bodyParams = new HashMap<String, String>();
        }
        this.bodyParams.put(name, value);
    }

    /**
     * 添加body参数
     *
     * @param name
     * @param value       可以是String, File, InputStream 或 byte[]
     * @param contentType 可为null
     */
    public void addBodyParameter(String name, Object value, String contentType) {
        if (this.fileParams == null) {
            this.fileParams = new HashMap<String, Object>();
        }
        if (TextUtils.isEmpty(contentType)) {
            this.fileParams.put(name, value);
        } else {
            this.fileParams.put(name, new ContentTypeWrapper<Object>(value, contentType));
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
     * 用json形式的bodyContent上传
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
        return bodyContent;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public HashMap<String, String> getQueryStringParams() {
        checkBodyParams();
        return queryStringParams;
    }

    public HashMap<String, String> getBodyParams() {
        checkBodyParams();
        return bodyParams;
    }

    public HashMap<String, Object> getFileParams() {
        return fileParams;
    }

    public HashMap<String, String> getStringParams() {
        HashMap<String, String> result = new HashMap<String, String>();
        if (queryStringParams != null) {
            result.putAll(queryStringParams);
        }
        if (bodyParams != null) {
            for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    public void clearParams() {
        if (queryStringParams != null) {
            queryStringParams.clear();
        }
        if (bodyParams != null) {
            bodyParams.clear();
        }
        if (fileParams != null) {
            fileParams.clear();
        }
        bodyContent = null;
        requestBody = null;
    }

    public void removeParameter(String name) {
        if (queryStringParams != null) {
            queryStringParams.remove(name);
        }
        if (bodyParams != null) {
            bodyParams.remove(name);
        }
        if (fileParams != null) {
            fileParams.remove(name);
        }
    }

    public String getStringParameter(String key) {
        if (queryStringParams != null && queryStringParams.containsKey(key)) {
            return queryStringParams.get(key);
        } else if (bodyParams != null && bodyParams.containsKey(key)) {
            Object value = bodyParams.get(key);
            return value == null ? null : value.toString();
        } else {
            return null;
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
        } else if (multipart || (fileParams != null && fileParams.size() > 0)) {
            if (!multipart && fileParams.size() == 1) {
                for (Object value : fileParams.values()) {
                    String contentType = null;
                    if (value instanceof ContentTypeWrapper) {
                        ContentTypeWrapper wrapper = (ContentTypeWrapper) value;
                        value = wrapper.getObject();
                        contentType = wrapper.getContentType();
                    }
                    if (value instanceof File) {
                        result = new FileBody((File) value, contentType);
                    } else if (value instanceof InputStream) {
                        result = new InputStreamBody((InputStream) value, contentType);
                    } else if (value instanceof byte[]) {
                        result = new InputStreamBody(new ByteArrayInputStream((byte[]) value), contentType);
                    }
                    break;
                }
            } else {
                multipart = true;
                result = new MultipartBody(fileParams, charset);
            }
        } else if (bodyParams != null && bodyParams.size() > 0) {
            result = new BodyParamsBody(bodyParams, charset);
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
                    Object value = field.get(this);
                    if (value != null) {
                        this.addParameter(field.getName(), value);
                    }
                } catch (IllegalAccessException ex) {
                    LogUtil.e(ex.getMessage(), ex);
                }
            }
        }

        addEntityParams2Map(type.getSuperclass());
    }

    private HttpRequest getHttpRequest() {
        if (httpRequest == null) {
            Class<?> thisCls = this.getClass();
            if (thisCls != RequestParams.class) {
                httpRequest = thisCls.getAnnotation(HttpRequest.class);
            }
        }

        return httpRequest;
    }

    private void checkBodyParams() {
        if (bodyParams != null && (!TextUtils.isEmpty(bodyContent) || requestBody != null)) {
            if (this.queryStringParams == null) {
                this.queryStringParams = new HashMap<String, String>();
            }
            for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    queryStringParams.put(key, value);
                }
            }

            bodyParams.clear();
            bodyParams = null;
        }

        if (bodyParams != null && (multipart || (fileParams != null && fileParams.size() > 0))) {
            if (this.fileParams == null) {
                this.fileParams = new HashMap<String, Object>();
            }
            fileParams.putAll(bodyParams);
            bodyParams.clear();
            bodyParams = null;
        }

        if (asJsonContent && bodyParams != null && !bodyParams.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> entry : bodyParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    try {
                        jsonObject.put(key, value);
                    } catch (JSONException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            setBodyContent(jsonObject.toString());
            bodyParams.clear();
            bodyParams = null;
        }
    }
}
