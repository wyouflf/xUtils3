package org.xutils.common;

import java.lang.reflect.Type;

/**
 * Created by wyouflf on 15/6/5.
 * 通用回调接口
 */
public interface Callback {

    /**
     * 基础回调接口
     * @param <ResultType> 实体类
     */
    public interface CommonCallback<ResultType> extends Callback {
        /**
         * 请求成功
         * @param result 返回结果
         */
        void onSuccess(ResultType result);

        /**
         * 请求失败
         * @param ex
         * @param isOnCallback
         */
        void onError(Throwable ex, boolean isOnCallback);

        /**
         * 请求关闭
         * @param cex
         */
        void onCancelled(CancelledException cex);

        /**
         * 请求结束
         */
        void onFinished();
    }

    public interface TypedCallback<ResultType> extends CommonCallback<ResultType> {
        Type getLoadType();
    }

    public interface CacheCallback<ResultType> extends CommonCallback<ResultType> {
        /**
         * 是否信任缓存数据
         * @param result 缓存结果
         * @return boolean 是否信任缓存
         */
        boolean onCache(ResultType result);
    }

    public interface ProxyCacheCallback<ResultType> extends CacheCallback<ResultType> {
        /**
         * 是否只读缓存
         * @return boolean true:都不到缓存缓存情况回调onSuccess(null) false 发起网络请求
         */
        boolean onlyCache();
    }

    /**
     * 自定义解析数据
     * @param <PrepareType> 源数据
     * @param <ResultType> 解析后数据
     */
    public interface PrepareCallback<PrepareType, ResultType> extends CommonCallback<ResultType> {
        ResultType prepare(PrepareType rawData);
    }

    /**
     * 带进度回调
     */
    public interface ProgressCallback<ResultType> extends CommonCallback<ResultType> {
        /**
         * 等待空闲线程
         */
        void onWaiting();

        /**
         * 开始执行
         */
        void onStarted();

        /**
         * 进行中
         * @param total 文件总大小(由服务器返回，默认为0)
         * @param current 当前加载进度
         * @param isDownloading 是否正在下载
         */
        void onLoading(long total, long current, boolean isDownloading);
    }

    public interface GroupCallback<ItemType> extends Callback {
        void onSuccess(ItemType item);

        void onError(ItemType item, Throwable ex, boolean isOnCallback);

        void onCancelled(ItemType item, CancelledException cex);

        void onFinished(ItemType item);

        void onAllFinished();
    }

    public interface Callable<ResultType> {
        void call(ResultType result);
    }

    public interface Cancelable {
        void cancel();

        boolean isCancelled();
    }

    public static class CancelledException extends RuntimeException {
        public CancelledException(String detailMessage) {
            super(detailMessage);
        }
    }
}
