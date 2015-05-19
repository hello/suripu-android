package is.hello.sense.util;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * A scheduler that wraps a state safe executor and another scheduler, and uses these objects
 * to ensure that work is only run within safe conditions. Used to ensure that no work is
 * done by activities and fragments that are paused or destroyed.
 *
 * @see is.hello.sense.ui.activities.SenseActivity
 * @see is.hello.sense.ui.common.InjectionFragment
 */
public class StateSafeScheduler extends Scheduler {
    private final StateSafeExecutor targetCoordinator;
    private final Scheduler targetScheduler;

    public StateSafeScheduler(@NonNull StateSafeExecutor targetExecutor, @NonNull Scheduler targetScheduler) {
        this.targetCoordinator = targetExecutor;
        this.targetScheduler = targetScheduler;
    }

    /**
     * Creates a ResumeScheduler targeting the main thread.
     */
    public StateSafeScheduler(@NonNull StateSafeExecutor targetExecutor) {
        this(targetExecutor, AndroidSchedulers.mainThread());
    }

    @Override
    public StateSafeWorker createWorker() {
        return new StateSafeWorker(targetCoordinator, targetScheduler.createWorker());
    }


    private static class StateSafeWorker extends Worker {
        private final CompositeSubscription compositeSubscription = new CompositeSubscription();
        private final StateSafeExecutor targetExecutor;
        private final Worker targetWorker;

        private StateSafeWorker(@NonNull StateSafeExecutor targetExecutor, @NonNull Worker targetWorker) {
            this.targetExecutor = targetExecutor;
            this.targetWorker = targetWorker;
        }

        @Override
        public Subscription schedule(Action0 action) {
            return schedule(action, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            Runnable work = () -> compositeSubscription.add(targetWorker.schedule(action, delayTime, unit));
            Subscription subscription = Subscriptions.create(() -> targetExecutor.cancelPending(work));
            compositeSubscription.add(subscription);
            targetExecutor.execute(work);
            return subscription;
        }

        @Override
        public void unsubscribe() {
            compositeSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return compositeSubscription.isUnsubscribed();
        }
    }


}
