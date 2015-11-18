package org.xutils.common.task;

import android.os.Looper;

import org.xutils.common.Callback;
import org.xutils.common.TaskController;
import org.xutils.common.util.LogUtil;
import org.xutils.x;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wyouflf on 15/6/5.
 * 异步任务的管理类
 */
public final class TaskControllerImpl implements TaskController {

    private TaskControllerImpl() {
    }

    private static TaskController instance;

    public static void registerInstance() {
        if (instance == null) {
            synchronized (TaskController.class) {
                if (instance == null) {
                    instance = new TaskControllerImpl();
                }
            }
        }
        x.Ext.setTaskController(instance);
    }

    /**
     * run task
     *
     * @param task
     * @param <T>
     * @return
     */
    @Override
    public <T> AbsTask<T> start(AbsTask<T> task) {
        TaskProxy<T> proxy = null;
        if (task instanceof TaskProxy) {
            proxy = (TaskProxy<T>) task;
        } else {
            proxy = new TaskProxy<T>(task);
        }
        try {
            proxy.doBackground();
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
        return proxy;
    }

    @Override
    public <T> T startSync(AbsTask<T> task) throws Throwable {
        T result = null;
        try {
            task.onWaiting();
            task.onStarted();
            result = task.doBackground();
            task.onSuccess(result);
        } catch (Callback.CancelledException cex) {
            task.onCancelled(cex);
        } catch (Throwable ex) {
            task.onError(ex, false);
            throw ex;
        } finally {
            task.onFinished();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbsTask<?>> Callback.Cancelable startTasks(
            final Callback.GroupCallback<T> groupCallback, final T... tasks) {

        if (tasks == null) {
            throw new IllegalArgumentException("task must not be null");
        }

        final Runnable callIfOnAllFinished = new Runnable() {
            private final int total = tasks.length;
            private final AtomicInteger count = new AtomicInteger(0);

            @Override
            public void run() {
                if (count.incrementAndGet() == total) {
                    if (groupCallback != null) {
                        groupCallback.onAllFinished();
                    }
                }
            }
        };

        for (final T task : tasks) {
            start(new TaskProxy(task) {
                @Override
                protected void onSuccess(Object result) {
                    super.onSuccess(result);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (groupCallback != null) {
                                groupCallback.onSuccess(task);
                            }
                        }
                    });
                }

                @Override
                protected void onCancelled(final Callback.CancelledException cex) {
                    super.onCancelled(cex);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (groupCallback != null) {
                                groupCallback.onCancelled(task, cex);
                            }
                        }
                    });
                }

                @Override
                protected void onError(final Throwable ex, final boolean isCallbackError) {
                    super.onError(ex, isCallbackError);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (groupCallback != null) {
                                groupCallback.onError(task, ex, isCallbackError);
                            }
                        }
                    });
                }

                @Override
                protected void onFinished() {
                    super.onFinished();
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (groupCallback != null) {
                                groupCallback.onFinished(task);
                            }
                            callIfOnAllFinished.run();
                        }
                    });
                }
            });
        }

        return new Callback.Cancelable() {

            @Override
            public void cancel() {
                for (T task : tasks) {
                    task.cancel();
                }
            }

            @Override
            public boolean isCancelled() {
                boolean isCancelled = true;
                for (T task : tasks) {
                    if (!task.isCancelled()) {
                        isCancelled = false;
                    }
                }
                return isCancelled;
            }
        };
    }

    @Override
    public void autoPost(Runnable runnable) {
        if (runnable == null) return;
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            TaskProxy.sHandler.post(runnable);
        }
    }

    /**
     * run in UI thread
     *
     * @param runnable
     */
    @Override
    public void post(Runnable runnable) {
        if (runnable == null) return;
        TaskProxy.sHandler.post(runnable);
    }

    /**
     * run in UI thread
     *
     * @param runnable
     * @param delayMillis
     */
    @Override
    public void postDelayed(Runnable runnable, long delayMillis) {
        if (runnable == null) return;
        TaskProxy.sHandler.postDelayed(runnable, delayMillis);
    }

    /**
     * run in background thread
     *
     * @param runnable
     */
    @Override
    public void run(Runnable runnable) {
        if (!TaskProxy.sDefaultExecutor.isBusy()) {
            TaskProxy.sDefaultExecutor.execute(runnable);
        } else {
            new Thread(runnable).start();
        }
    }

    @Override
    public void removeCallbacks(Runnable runnable) {
        TaskProxy.sHandler.removeCallbacks(runnable);
    }
}
