package org.xutils.common.task;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wyouflf on 15/6/5.
 * 带有优先级的Runnable类型(仅在task包内可用)
 */
/*package*/ class PriorityRunnable implements Runnable {

    private static final AtomicLong SEQ_SEED = new AtomicLong(0);
    /*package*/ final long SEQ = SEQ_SEED.getAndIncrement();

    public final Priority priority;
    private final Runnable runnable;

    public PriorityRunnable(Priority priority, Runnable runnable) {
        this.priority = priority == null ? Priority.DEFAULT : priority;
        this.runnable = runnable;
    }

    @Override
    public final void run() {
        this.runnable.run();
    }
}
