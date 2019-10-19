package org.xutils.http;

import android.content.Context;
import android.text.TextUtils;

import org.xutils.common.task.Priority;
import org.xutils.http.annotation.HttpRequest;
import org.xutils.http.app.DefaultParamsBuilder;
import org.xutils.http.app.DefaultRedirectHandler;
import org.xutils.http.app.HttpRetryHandler;
import org.xutils.http.app.ParamsBuilder;
import org.xutils.http.app.RedirectHandler;
import org.xutils.http.app.RequestTracker;
import org.xutils.x;

import java.net.Proxy;
import java.util.concurrent.Executor;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by wyouflf on 15/7/17.
 * 网络请求参数实体
 */
public class RequestParams extends BaseParams {

    public final static int MAX_FILE_LOAD_WORKER = 10;
    private final static DefaultRedirectHandler DEFAULT_REDIRECT_HANDLER = new DefaultRedirectHandler();

    // 注解及其扩展参数
    private HttpRequest httpRequest;
    private String uri;
    private final String[] signs;
    private final String[] cacheKeys;
    private ParamsBuilder builder;
    private String buildUri;
    private String buildCacheKey;
    private SSLSocketFactory sslSocketFactory;

    // 扩展参数
    private Context context;
    private Proxy proxy; // 代理
    private HostnameVerifier hostnameVerifier; // https域名校验
    private boolean useCookie = true; // 是否在请求过程中启用cookie
    private String cacheDirName; // 缓存文件夹名称
    private long cacheSize; // 缓存文件夹大小
    private long cacheMaxAge; // 默认缓存存活时间, 单位:毫秒.(如果服务没有返回有效的max-age或Expires)
    private Executor executor; // 自定义线程池
    private Priority priority = Priority.DEFAULT; // 请求优先级
    private int connectTimeout = 1000 * 15; // 连接超时时间
    private int readTimeout = 1000 * 15; // 读取超时时间
    private boolean autoResume = true; // 是否在下载是自动断点续传
    private boolean autoRename = false; // 是否根据头信息自动命名文件
    private int maxRetryCount = 2; // 最大请求错误重试次数
    private String saveFilePath; // 下载文件时文件保存的路径和文件名
    private boolean cancelFast = false; // 是否可以被立即停止, true: 为请求创建新的线程, 取消时请求线程被立即中断.
    private int loadingUpdateMaxTimeSpan = 300; // 进度刷新最大间隔时间(ms)
    private HttpRetryHandler httpRetryHandler; // 自定义HttpRetryHandler
    private RequestTracker requestTracker; // 自定义日志记录接口.
    private RedirectHandler redirectHandler = DEFAULT_REDIRECT_HANDLER;

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
     * @param builder   用于自定义参数构建过程, 为空时使用{@link DefaultParamsBuilder}
     * @param signs     自定义需要签名的字段, 会传给ParamsBuilder
     * @param cacheKeys 自定义缓存关键key信息, 会传给ParamsBuilder
     */
    public RequestParams(String uri, ParamsBuilder builder, String[] signs, String[] cacheKeys) {
        if (uri != null && builder == null) {
            builder = new DefaultParamsBuilder();
        }
        this.uri = uri;
        this.signs = signs;
        this.cacheKeys = cacheKeys;
        this.builder = builder;
        this.context = x.app();
    }

    // invoke via HttpTask#createNewRequest
    /*package*/ void init() throws Throwable {
        if (!TextUtils.isEmpty(buildUri)) return;

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
            buildUri = builder.buildUri(this, httpRequest);
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

    public void setUri(String uri) {
        if (TextUtils.isEmpty(buildUri)) {
            this.uri = uri;
        } else {
            this.buildUri = uri;
        }
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

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    /**
     * 是否在请求过程中启用cookie, 默认true.
     */
    public boolean isUseCookie() {
        return useCookie;
    }

    /**
     * 是否在请求过程中启用cookie, 默认true.
     */
    public void setUseCookie(boolean useCookie) {
        this.useCookie = useCookie;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
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

    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * 注意get请求失败后默认会重试2次, 可以通过setMaxRetryCount(0)来防止get请求自动重试.
     */
    public void setReadTimeout(int readTimeout) {
        if (readTimeout > 0) {
            this.readTimeout = readTimeout;
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
     */
    public long getCacheMaxAge() {
        return cacheMaxAge;
    }

    /**
     * 默认缓存存活时间, 单位:毫秒.(如果服务没有返回有效的max-age或Expires)
     */
    public void setCacheMaxAge(long cacheMaxAge) {
        this.cacheMaxAge = cacheMaxAge;
    }

    /**
     * 自定义线程池
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * 自定义线程池
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

    public int getLoadingUpdateMaxTimeSpan() {
        return loadingUpdateMaxTimeSpan;
    }

    /**
     * 进度刷新最大间隔时间(默认300毫秒)
     */
    public void setLoadingUpdateMaxTimeSpan(int loadingUpdateMaxTimeSpan) {
        this.loadingUpdateMaxTimeSpan = loadingUpdateMaxTimeSpan;
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
     */
    public void setRedirectHandler(RedirectHandler redirectHandler) {
        this.redirectHandler = redirectHandler;
    }

    public RequestTracker getRequestTracker() {
        return requestTracker;
    }

    public void setRequestTracker(RequestTracker requestTracker) {
        this.requestTracker = requestTracker;
    }

    private void initEntityParams() {
        RequestParamsHelper.parseKV(this, this.getClass(), new RequestParamsHelper.ParseKVListener() {
            @Override
            public void onParseKV(String name, Object value) {
                addParameter(name, value);
            }
        });
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

    @Override
    public String toString() {
        String url = this.getUri();
        String baseParamsStr = super.toString();
        return TextUtils.isEmpty(url) ?
                baseParamsStr :
                url + (url.contains("?") ? "&" : "?") + baseParamsStr;
    }
}
