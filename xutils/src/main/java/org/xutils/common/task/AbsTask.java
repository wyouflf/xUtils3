package org.xutils.common.task;

import org.xutils.common.Callback;

import java.util.concurrent.Executor;


/**
 * Created by wyouflf on 15/6/5.
 * 异步任务基类
 *
 * @param <ResultType>
 */
public abstract class AbsTask<ResultType> implements Callback.Cancelable {

    private TaskProxy taskProxy = null;
    private final Callback.Cancelable cancelHandler;

    private volatile boolean isCancelled = false;
    private volatile State state = State.IDLE;
    private ResultType result;

    public AbsTask() {
        this(null);
    }

    public AbsTask(Callback.Cancelable cancelHandler) {
        this.cancelHandler = cancelHandler;
    }

    protected abstract ResultType doBackground() throws Throwable;

    protected abstract void onSuccess(ResultType result);

    protected abstract void onError(Throwable ex, boolean isCallbackError);

    protected void onWaiting() {
    }

    protected void onStarted() {
    }

    protected void onUpdate(int flag, Object... args) {
    }

    protected void onCancelled(Callback.CancelledException cex) {
    }

    protected void onFinished() {
    }

    public Priority getPriority() {
        return null;
    }

    public Executor getExecutor() {
        return null;
    }

    protected final void update(int flag, Object... args) {
        if (taskProxy != null) {
            taskProxy.onUpdate(flag, args);
        }
    }

    /**
     * invoked via cancel()
     */
    protected void cancelWorks() {
    }

    @Override
    public final synchronized void cancel() {
        if (!this.isCancelled) {
            this.isCancelled = true;
            cancelWorks();
            if (cancelHandler != null && !cancelHandler.isCancelled()) {
                cancelHandler.cancel();
            }
            if (this.state == State.WAITING) {
                if (taskProxy != null) {
                    taskProxy.onCancelled(new Callback.CancelledException("cancelled by user"));
                    taskProxy.onFinished();
                } else if (this instanceof TaskProxy) {
                    this.onCancelled(new Callback.CancelledException("cancelled by user"));
                    this.onFinished();
                }
            }
        }
    }

    @Override
    public final boolean isCancelled() {
        return isCancelled || state == State.CANCELLED ||
                (cancelHandler != null && cancelHandler.isCancelled());
    }

    public final boolean isFinished() {
        return this.state.value() > State.STARTED.value();
    }

    public final State getState() {
        return state;
    }

    public final ResultType getResult() {
        return result;
    }

    /*package*/
    void setState(State state) {
        this.state = state;
    }

    /*package*/
    final void setTaskProxy(TaskProxy taskProxy) {
        this.taskProxy = taskProxy;
    }

    /*package*/
    final void setResult(ResultType result) {
        this.result = result;
    }

    public enum State {
        IDLE(0), WAITING(1), STARTED(2), SUCCESS(3), CANCELLED(4), ERROR(5);
        private final int value;

        private State(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }
}
