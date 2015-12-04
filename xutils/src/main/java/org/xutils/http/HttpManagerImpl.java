package org.xutils.http;

import android.text.TextUtils;

import org.xutils.HttpManager;
import org.xutils.common.Callback;
import org.xutils.x;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Created by wyouflf on 15/7/23.
 * HttpManager实现
 */
public final class HttpManagerImpl implements HttpManager {

    private static final Object lock = new Object();
    private static HttpManagerImpl instance;
    private static final HashMap<String, HttpTask<?>> DOWNLOAD_TASK = new HashMap<String, HttpTask<?>>(1);

    private HttpManagerImpl() {
    }

    public static void registerInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new HttpManagerImpl();
                }
            }
        }
        x.Ext.setHttpManager(instance);
    }

    @Override
    public <T> Callback.Cancelable get(RequestParams entity, Callback.CommonCallback<T> callback) {
        return request(HttpMethod.GET, entity, callback);
    }

    @Override
    public <T> Callback.Cancelable post(RequestParams entity, Callback.CommonCallback<T> callback) {
        return request(HttpMethod.POST, entity, callback);
    }

    @Override
    public <T> Callback.Cancelable request(HttpMethod method, RequestParams entity, Callback.CommonCallback<T> callback) {
        final String saveFilePath = entity.getSaveFilePath();
        if (!TextUtils.isEmpty(saveFilePath)) {
            HttpTask<?> task = DOWNLOAD_TASK.get(saveFilePath);
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
        entity.setMethod(method);
        Callback.Cancelable cancelable = null;
        if (callback instanceof Callback.Cancelable) {
            cancelable = (Callback.Cancelable) callback;
        }
        HttpTask<T> task = null;
        if (!TextUtils.isEmpty(saveFilePath)) {
            task = new HttpTask<T>(entity, cancelable, callback) {
                @Override
                protected void onFinished() {
                    super.onFinished();
                    synchronized (DOWNLOAD_TASK) {
                        HttpTask<?> task = DOWNLOAD_TASK.get(saveFilePath);
                        if (task == this) {
                            DOWNLOAD_TASK.remove(saveFilePath);
                        }
                    }
                }
            };
            synchronized (DOWNLOAD_TASK) {
                DOWNLOAD_TASK.put(saveFilePath, task);
            }
        } else {
            task = new HttpTask<T>(entity, cancelable, callback);
        }
        return x.task().start(task);
    }

    @Override
    public <T> T getSync(RequestParams entity, Class<T> resultType) throws Throwable {
        return requestSync(HttpMethod.GET, entity, resultType);
    }

    @Override
    public <T> T postSync(RequestParams entity, Class<T> resultType) throws Throwable {
        return requestSync(HttpMethod.POST, entity, resultType);
    }

    @Override
    public <T> T requestSync(HttpMethod method, RequestParams entity, Class<T> resultType) throws Throwable {
        entity.setMethod(method);
        SyncCallback<T> callback = new SyncCallback<T>(resultType);
        HttpTask<T> task = new HttpTask<T>(entity, null, callback);
        return x.task().startSync(task);
    }

    private class SyncCallback<T> implements Callback.TypedCallback<T> {

        private final Class<T> resultType;

        public SyncCallback(Class<T> resultType) {
            this.resultType = resultType;
        }

        @Override
        public Type getResultType() {
            return resultType;
        }

        @Override
        public void onSuccess(T result) {

        }

        @Override
        public void onError(Throwable ex, boolean isOnCallback) {

        }

        @Override
        public void onCancelled(CancelledException cex) {

        }

        @Override
        public void onFinished() {

        }
    }
}
