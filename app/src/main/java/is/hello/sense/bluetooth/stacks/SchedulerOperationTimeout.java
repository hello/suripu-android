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
    private final Pool pool;

    private boolean isInPool = true;

    private String name;
    private long durationMs;

    private @Nullable Action0 action;
    private @Nullable Scheduler scheduler;
    private @Nullable Subscription subscription;


    //region Lifecycle

    private SchedulerOperationTimeout(@NonNull Pool pool) {
        this.pool = pool;
    }

    private SchedulerOperationTimeout init(@NonNull String name, long duration, @NonNull TimeUnit timeUnit) {
        this.name = name;
        this.durationMs = timeUnit.toMillis(duration);
        this.isInPool = false;

        return this;
    }

    //endregion


    @Override
    public void schedule() {
        if (isInPool) {
            throw new IllegalStateException("Cannot schedule a recycled time out");
        }

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
            throw new IllegalStateException("Recycle called on already recycled timeout '" + name + "'");
        }

        Logger.info(LOG_TAG, "Recycling time out '" + name + "'");

        if (subscription != null) {
            unschedule();
            throw new IllegalStateException("Recycle called on scheduled operation.");
        }

        this.durationMs = 0;
        this.action = null;
        this.scheduler = null;

        this.isInPool = true;
        pool.recycle(this);
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


    public static class Pool implements OperationTimeout.Pool {
        private final String name;
        private final List<SchedulerOperationTimeout> timeouts;

        public Pool(@NonNull String name, int size) {
            this.name = name;
            this.timeouts = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                timeouts.add(new SchedulerOperationTimeout(this));
            }
        }


        @Override
        public OperationTimeout acquire(@NonNull String name, long duration, TimeUnit timeUnit) {
            Logger.info(LOG_TAG, "Vending time out '" + name + "' (" + timeouts.size() + " objects available in '" + this.name + "')");

            if (timeouts.isEmpty()) {
                throw new IllegalStateException("Pool '" + this.name + "' exhausted, cannot vend '" + name + "'");
            }

            SchedulerOperationTimeout timeout = timeouts.get(0);
            timeouts.remove(0);

            Logger.info(LOG_TAG, "Pool now contains " + timeouts.size() + " available in '" + this.name + "')");

            return timeout.init(name, duration, timeUnit);
        }

        private void recycle(@NonNull SchedulerOperationTimeout timeout) {
            timeouts.add(timeout);
            Logger.info(LOG_TAG, "Pool now contains " + timeouts.size() + " available in '" + this.name + "')");
        }


        @Override
        public String toString() {
            return "Pool{" +
                    "name='" + name + '\'' +
                    ", availableObjects=" + timeouts.size() +
                    '}';
        }
    }
}
