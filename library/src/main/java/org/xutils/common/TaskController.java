package org.xutils.common;

import org.xutils.common.task.AbsTask;

/**
 * Created by wyouflf on 15/6/11.
 * 任务管理接口
 */
public interface TaskController {

    /**
     * 在UI线程执行runnable.
     * 如果已在UI线程, 则直接执行.
     *
     * @param runnable
     */
    void autoPost(Runnable runnable);

    /**
     * 在UI线程执行runnable.
     * post到msg queue.
     *
     * @param runnable
     */
    void post(Runnable runnable);

    /**
     * 在UI线程执行runnable.
     *
     * @param runnable
     * @param delayMillis 延迟时间(单位毫秒)
     */
    void postDelayed(Runnable runnable, long delayMillis);

    /**
     * 在后台线程执行runnable
     *
     * @param runnable
     */
    void run(Runnable runnable);

    /**
     * 移除未执行的runnable
     *
     * @param runnable
     */
    void removeCallbacks(Runnable runnable);

    <T> AbsTask<T> start(AbsTask<T> task);

    <T> T startSync(AbsTask<T> task) throws Throwable;

    <T extends AbsTask<?>> Callback.Cancelable startTasks(Callback.GroupCallback<T> groupCallback, T... tasks);
}
