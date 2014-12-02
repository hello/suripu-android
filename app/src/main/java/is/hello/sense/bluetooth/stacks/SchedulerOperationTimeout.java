package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import is.hello.sense.util.Logger;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

/**
 * Simple implementation of {@see OperationTimeout} that uses deferred workers.
 */
public final class SchedulerOperationTimeout implements OperationTimeout {
    /**
     * One timeout for stack operations, one timeout for a wrapper interface.
     */
    static final int OBJECT_POOL_SIZE = 2;

    private boolean isInPool = true;

    private String name;
    private long durationMs;

    private @Nullable Action0 action;
    private @Nullable Scheduler scheduler;
    private @Nullable Subscription subscription;


    //region Lifecycle

    private SchedulerOperationTimeout() {

    }

    private SchedulerOperationTimeout init(@NonNull String name, long duration, @NonNull TimeUnit timeUnit) {
        this.name = name;
        this.durationMs = timeUnit.toMillis(duration);
        this.isInPool = false;

        return this;
    }

    private static final List<SchedulerOperationTimeout> pool = new ArrayList<>(OBJECT_POOL_SIZE);
    static {
        for (int i = 0; i < OBJECT_POOL_SIZE; i++) {
            pool.add(new SchedulerOperationTimeout());
        }
    }

    public static SchedulerOperationTimeout acquire(@NonNull String name, long duration, @NonNull TimeUnit timeUnit) {
        Logger.info(LOG_TAG, "Vending time out '" + name + "' (" + pool.size() + " objects available)");

        if (pool.isEmpty()) {
            throw new IllegalStateException("SchedulerOperationTimeout object pool exhausted, cannot vend '" + name + "'");
        }

        SchedulerOperationTimeout timeout = pool.get(0);
        pool.remove(0);

        Logger.info(LOG_TAG, "Pool now contains " + pool.size() + " available objects");

        return timeout.init(name, duration, timeUnit);
    }

    //endregion


    @Override
    public void schedule() {
        if (action == null || scheduler == null) {
            throw new IllegalStateException("Cannot schedule a time out that has no action");
        }

        Logger.info(LOG_TAG, "Scheduling time out '" + name + "'");

        if (subscription != null && !subscription.isUnsubscribed()) {
            unschedule();
        }

        // It's the responsibility of the scheduler of the timeout
        // to clean up after it when a timeout condition occurs.
        this.subscription = scheduler.createWorker().schedule(action, durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unschedule() {
        if (subscription != null) {
            Logger.info(LOG_TAG, "Unscheduling time out '" + name + "'");

            subscription.unsubscribe();
            this.subscription = null;
        }
    }

    @Override
    public void reschedule() {
        unschedule();
        schedule();
    }

    @Override
    public void setTimeoutAction(@NonNull Action0 action, @NonNull Scheduler scheduler) {
        this.action = action;
        this.scheduler = scheduler;
    }

    @Override
    public void recycle() {
        if (isInPool) {
            throw new IllegalStateException("Recycle called on already recycled timeout");
        }

        Logger.info(LOG_TAG, "Recycling time out '" + name + "'");

        if (subscription != null) {
            unschedule();
            throw new IllegalStateException("Recycle called on scheduled operation.");
        }

        this.name = null;
        this.durationMs = 0;
        this.action = null;
        this.scheduler = null;

        this.isInPool = true;
        pool.add(0, this);

        Logger.info(LOG_TAG, "Pool now contains " + pool.size() + " available objects");
    }


    @Override
    public String toString() {
        return "SchedulerOperationTimeout{" +
                "name='" + name + '\'' +
                ", durationMs=" + durationMs +
                ", action=" + action +
                ", scheduler=" + scheduler +
                ", subscription=" + subscription +
                '}';
    }
}
