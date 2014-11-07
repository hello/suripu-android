package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import is.hello.sense.util.Logger;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

/**
 * Simple implementation of {@see OperationTimeout} that uses deferred workers.
 */
public class SchedulerOperationTimeout implements OperationTimeout {
    private final String name;
    private final long durationMs;

    private @Nullable Action0 action;
    private @Nullable Scheduler scheduler;
    private @Nullable Subscription subscription;


    public SchedulerOperationTimeout(@NonNull String name, long duration, @NonNull TimeUnit timeUnit) {
        this.name = name;
        this.durationMs = timeUnit.toMillis(duration);
    }


    @Override
    public void schedule() {
        if (action == null || scheduler == null) {
            throw new NullPointerException();
        }

        Logger.info(LOG_TAG, "Scheduling time out " + toString());

        if (subscription != null && !subscription.isUnsubscribed())
            unschedule();

        this.subscription = scheduler.createWorker().schedule(action, durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unschedule() {
        if (action == null || scheduler == null) {
            throw new NullPointerException();
        }

        Logger.info(LOG_TAG, "Unscheduling time out " + toString());

        if (subscription != null) {
            subscription.unsubscribe();
            this.subscription = null;
        }
    }

    @Override
    public boolean isScheduled() {
        return (subscription != null);
    }

    @Override
    public void setTimeoutAction(@NonNull Action0 action, @NonNull Scheduler scheduler) {
        Logger.info(LOG_TAG, "Timeout action " + action + "; " + scheduler);

        this.action = action;
        this.scheduler = scheduler;
    }

    @Override
    public void recycle() {
        Logger.info(LOG_TAG, "Recycling time out " + toString());

        if (subscription != null) {
            unschedule();
            throw new IllegalStateException("Recycle called on scheduled operation.");
        }

        this.action = null;
        this.scheduler = null;
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
