package org.xutils.http;

import org.xutils.common.Callback;
import org.xutils.common.task.AbsTask;
import org.xutils.common.task.Priority;
import org.xutils.common.task.PriorityExecutor;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.common.util.ParameterizedTypeUtil;
import org.xutils.http.app.ResponseTracker;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wyouflf on 15/7/23.
 * http 请求任务
 */
public class HttpTask<ResultType> extends AbsTask<ResultType> implements ProgressCallbackHandler {

    // 请求相关
    private UriRequest request;
    private RequestWorker requestWorker;
    private final RequestParams params;
    private final Executor executor;
    private final Callback.CommonCallback<ResultType> callback;

    // 缓存控制
    private Object rawResult = null;
    private final Object cacheLock = new Object();
    private volatile Boolean trustCache = null;

    // 扩展callback
    private Callback.CacheCallback<ResultType> cacheCallback;
    private Callback.PrepareCallback prepareCallback;
    private Callback.ProgressCallback progressCallback;

    // 日志追踪
    private ResponseTracker tracker;

    // 文件下载线程数限制
    private Type loadType;
    private final static int MAX_FILE_LOAD_WORKER = 3;
    private final static AtomicInteger sCurrFileLoadCount = new AtomicInteger(0);

    private static final PriorityExecutor HTTP_EXECUTOR = new PriorityExecutor(5);
    private static final PriorityExecutor CACHE_EXECUTOR = new PriorityExecutor(5);


    public HttpTask(RequestParams params, Callback.Cancelable cancelHandler,
                    Callback.CommonCallback<ResultType> callback) {
        super(cancelHandler);

        assert params != null;
        assert callback != null;

        // set params & callback
        this.params = params;
        this.callback = callback;
        if (callback instanceof Callback.CacheCallback) {
            this.cacheCallback = (Callback.CacheCallback<ResultType>) callback;
        }
        if (callback instanceof Callback.PrepareCallback) {
            this.prepareCallback = (Callback.PrepareCallback) callback;
        }
        if (callback instanceof Callback.ProgressCallback) {
            this.progressCallback = (Callback.ProgressCallback<ResultType>) callback;
        }

        // init executor
        if (params.getExecutor() != null) {
            this.executor = params.getExecutor();
        } else {
            if (cacheCallback != null) {
                this.executor = CACHE_EXECUTOR;
            } else {
                this.executor = HTTP_EXECUTOR;
            }
        }
    }

    // 初始化请求参数
    private UriRequest initRequest() throws Throwable {
        // get loadType
        Class<?> callBackType = callback.getClass();
        if (callback instanceof Callback.TypedCallback) {
            loadType = ((Callback.TypedCallback) callback).getResultType();
        } else if (callback instanceof Callback.PrepareCallback) {
            loadType = ParameterizedTypeUtil.getParameterizedType(callBackType, Callback.PrepareCallback.class, 0);
        } else {
            loadType = ParameterizedTypeUtil.getParameterizedType(callBackType, Callback.CommonCallback.class, 0);
        }

        // check loadType & resultType
        {
            if (loadType instanceof ParameterizedType) {
                loadType = ((ParameterizedType) loadType).getRawType();
            } else if (loadType instanceof TypeVariable) {
                throw new IllegalArgumentException(
                        "not support callback type" + callBackType.getCanonicalName());
            }
        }

        UriRequest result = new UriRequest(params, (Class<?>) loadType);
        result.setProgressCallbackHandler(this);
        result.setCallingClassLoader(callBackType.getClassLoader());

        if (callback instanceof ResponseTracker) {
            tracker = (ResponseTracker) callback;
        } else {
            tracker = result.getResponseTracker();
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ResultType doBackground() throws Throwable {

        if (this.isCancelled()) {
            throw new Callback.CancelledException("cancelled before request");
        }

        // 初始化请求参数
        ResultType result = null;
        boolean retry = true;
        int retryCount = 0;
        Throwable exception = null;
        HttpRetryHandler retryHandler = new HttpRetryHandler(this.params.getMaxRetryCount());
        request = initRequest();
        if (tracker != null) {
            tracker.onStart(request);
        }

        if (this.isCancelled()) {
            throw new Callback.CancelledException("cancelled before request");
        }

        // 检查缓存
        Object cacheResult = null;
        if (cacheCallback != null) {
            // 尝试从缓存获取结果, 并为请求头加入缓存控制参数.
            while (retry) {
                try {
                    clearRawResult();
                    rawResult = this.request.loadResultFromCache();
                    break;
                } catch (Throwable ex) {
                    LogUtil.w("load disk cache error", ex);
                    exception = ex;
                    retry = retryHandler.retryRequest(ex, ++retryCount, this.request);
                }
            }

            if (this.isCancelled()) {
                clearRawResult();
                throw new Callback.CancelledException("cancelled before request");
            }

            if (rawResult != null) {
                if (prepareCallback != null) {
                    try {
                        cacheResult = prepareCallback.prepare(rawResult);
                    } catch (Throwable ex) {
                        cacheResult = null;
                        LogUtil.w("prepare disk cache error", ex);
                    } finally {
                        clearRawResult();
                    }
                } else {
                    cacheResult = rawResult;
                }

                if (this.isCancelled()) {
                    throw new Callback.CancelledException("cancelled before request");
                }

                if (cacheResult != null) {
                    // 同步等待是否信任缓存
                    this.update(FLAG_CACHE, cacheResult);
                    while (trustCache == null) {
                        synchronized (cacheLock) {
                            try {
                                cacheLock.wait();
                            } catch (Throwable ignored) {
                            }
                        }
                    }

                    // 处理完成
                    if (trustCache) {
                        return null;
                    }
                }
            }
        }

        if (trustCache == null) {
            trustCache = false;
        }

        if (cacheResult == null) {
            this.request.clearCacheHeader();
        }

        // 发起请求
        retry = true;
        while (retry) {

            try {
                if (this.isCancelled()) {
                    throw new Callback.CancelledException("cancelled before request");
                }

                // 由loader发起请求, 拿到结果.
                this.request.close(); // retry 前关闭上次请求

                try {
                    clearRawResult();
                    requestWorker = new RequestWorker(this.request, this.loadType);
                    if (params.isCancelFast()) {
                        requestWorker.start();
                        requestWorker.join();
                    } else {
                        requestWorker.run();
                    }
                    if (requestWorker.ex != null) {
                        throw requestWorker.ex;
                    }
                    rawResult = requestWorker.result;
                } catch (Throwable ex) {
                    clearRawResult();
                    if (this.isCancelled()) {
                        throw new Callback.CancelledException("cancelled during request");
                    } else {
                        throw ex;
                    }
                }

                if (prepareCallback != null) {
                    try {
                        result = (ResultType) prepareCallback.prepare(rawResult);
                    } finally {
                        clearRawResult();
                    }
                } else {
                    result = (ResultType) rawResult;
                }

                // 保存缓存
                if (cacheCallback != null) {
                    try {
                        this.request.save2Cache();
                    } catch (Throwable ex) {
                        LogUtil.e("save2Cache", ex);
                    }
                }

                retry = false;

                if (this.isCancelled()) {
                    throw new Callback.CancelledException("cancelled after request");
                }
            } catch (Throwable ex) {
                if (this.request.getResponseCode() == 304) { // disk cache is valid.
                    return null;
                } else {
                    exception = ex;
                    retry = retryHandler.retryRequest(ex, ++retryCount, this.request);
                }
            }

        }

        if (exception != null && result == null && !trustCache) {
            throw exception;
        }

        return result;
    }

    private static final int FLAG_CACHE = 1;
    private static final int FLAG_PROGRESS = 2;

    @Override
    @SuppressWarnings("unchecked")
    protected void onUpdate(int flag, Object... args) {
        switch (flag) {
            case FLAG_CACHE: {
                synchronized (cacheLock) {
                    try {
                        trustCache = this.cacheCallback.onCache((ResultType) args[0]);
                    } catch (Throwable ex) {
                        trustCache = false;
                        callback.onError(ex, true);
                    } finally {
                        cacheLock.notifyAll();
                    }
                }
            }
            case FLAG_PROGRESS: {
                if (this.progressCallback != null && args.length == 3) {
                    try {
                        this.progressCallback.onLoading(
                                ((Number) args[0]).longValue(),
                                ((Number) args[1]).longValue(),
                                (Boolean) args[2]);
                    } catch (Throwable ex) {
                        callback.onError(ex, true);
                    }
                }
            }
            default: {
                break;
            }
        }
    }

    @Override
    protected void onWaiting() {
        if (progressCallback != null) {
            progressCallback.onWaiting();
        }
    }

    @Override
    protected void onStarted() {
        if (progressCallback != null) {
            progressCallback.onStarted();
        }
    }

    @Override
    protected void onSuccess(ResultType result) {
        if (result != null) {
            callback.onSuccess(result);
        }
        if (tracker != null) {
            tracker.onSuccess(request);
        }
    }

    @Override
    protected void onError(Throwable ex, boolean isCallbackError) {
        if (tracker != null) {
            tracker.onError(request, ex, isCallbackError);
        }
        callback.onError(ex, isCallbackError);
    }


    @Override
    protected void onCancelled(Callback.CancelledException cex) {
        callback.onCancelled(cex);
        if (tracker != null) {
            tracker.onCancelled(request);
        }
    }

    @Override
    protected void onFinished() {
        clearRawResult();
        IOUtil.closeQuietly(request);
        callback.onFinished();
    }

    private void clearRawResult() {
        if (rawResult instanceof Closeable) {
            IOUtil.closeQuietly((Closeable) rawResult);
        }
        rawResult = null;
    }

    @Override
    protected void cancelWorks() {
        if (requestWorker != null && params.isCancelFast()) {
            requestWorker.interrupt();
        }
        IOUtil.closeQuietly(request);
    }

    @Override
    public Executor getExecutor() {
        return this.executor;
    }

    @Override
    public Priority getPriority() {
        return params.getPriority();
    }

    // ############################### start: region implements ProgressCallbackHandler
    private final static long RATE = 300; // 0.3s
    private long lastUpdateTime;

    /**
     * @param total
     * @param current
     * @param forceUpdateUI
     * @return continue
     */
    @Override
    public boolean updateProgress(long total, long current, boolean forceUpdateUI) {

        if (isCancelled() || isFinished()) {
            return false;
        }

        if (progressCallback != null && request != null && total > 0) {
            if (forceUpdateUI) {
                this.update(FLAG_PROGRESS, total, current, request.isLoading());
            } else {
                long currTime = System.currentTimeMillis();
                if (currTime - lastUpdateTime >= RATE) {
                    lastUpdateTime = currTime;
                    this.update(FLAG_PROGRESS, total, current, request.isLoading());
                }
            }
        }

        return !isCancelled() && !isFinished();
    }

    // ############################### end: region implements ProgressCallbackHandler

    @Override
    public String toString() {
        return params.toString();
    }


    /**
     * 请求发送和加载数据线程.
     * 该线程被join到HttpTask的工作线程去执行.
     * 它的主要作用是为了能强行中断请求的链接过程;
     * 并辅助限制同时下载文件的线程数.
     * but:
     * 创建一个Thread约耗时2毫秒, 优化?
     */
    private final class RequestWorker extends Thread {
        private final UriRequest request;
        private final Type loadType;
        private Object result;
        private Throwable ex;

        private RequestWorker(UriRequest request, Type loadType) {
            this.request = request;
            this.loadType = loadType;
        }

        public void run() {
            try {
                if (File.class == loadType) {
                    while (sCurrFileLoadCount.get() >= MAX_FILE_LOAD_WORKER
                            && !HttpTask.this.isCancelled()) {
                        synchronized (sCurrFileLoadCount) {
                            try {
                                sCurrFileLoadCount.wait();
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    sCurrFileLoadCount.incrementAndGet();
                }

                if (HttpTask.this.isCancelled()) {
                    throw new Callback.CancelledException("cancelled before request");
                }

                this.result = request.loadResult();
            } catch (Throwable ex) {
                this.ex = ex;
            } finally {
                if (File.class == loadType) {
                    synchronized (sCurrFileLoadCount) {
                        sCurrFileLoadCount.decrementAndGet();
                        sCurrFileLoadCount.notifyAll();
                    }
                }
            }
        }
    }

}
