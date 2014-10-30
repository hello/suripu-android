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
 * A scheduler that wraps another scheduler, and an object wrapping Resumable.
 * A Resumable object is one on which certain operations are unsafe while it is paused.
 * Examples of a Resumable object are {@see is.hello.sense.ui.activities.SenseActivity}
 * and {@see is.hello.sense.ui.common.InjectionFragment}. The scheduler sends all of
 * its workers through a Resumable object, which will then either run the worker
 * immediately if it's safe, or defer its execution until it's safe to do so.
 *
 * @see is.hello.sense.ui.activities.SenseActivity
 * @see is.hello.sense.ui.common.InjectionFragment
 */
public class ResumeScheduler extends Scheduler {
    private final Resumable targetResumable;
    private final Scheduler targetScheduler;

    public ResumeScheduler(@NonNull Resumable targetResumable, @NonNull Scheduler targetScheduler) {
        this.targetResumable = targetResumable;
        this.targetScheduler = targetScheduler;
    }

    /**
     * Creates a ResumeScheduler targeting the main thread.
     */
    public ResumeScheduler(@NonNull Resumable targetResumable) {
        this(targetResumable, AndroidSchedulers.mainThread());
    }

    @Override
    public ResumeWorker createWorker() {
        return new ResumeWorker(targetResumable, targetScheduler.createWorker());
    }


    private static class ResumeWorker extends Worker {
        private final CompositeSubscription compositeSubscription = new CompositeSubscription();
        private final Resumable targetResumable;
        private final Worker targetWorker;

        private ResumeWorker(@NonNull Resumable targetResumable, @NonNull Worker targetWorker) {
            this.targetResumable = targetResumable;
            this.targetWorker = targetWorker;
        }

        @Override
        public Subscription schedule(Action0 action) {
            return schedule(action, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            Runnable work = () -> compositeSubscription.add(targetWorker.schedule(action, delayTime, unit));
            Subscription subscription = Subscriptions.create(() -> targetResumable.cancelPostOnResume(work));
            compositeSubscription.add(subscription);
            targetResumable.postOnResume(work);
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


    /**
     * A class which has a paused state where certain work is unsafe.
     *
     * @see is.hello.sense.ui.activities.SenseActivity
     * @see is.hello.sense.ui.common.InjectionFragment
     */
    public interface Resumable {
        /**
         * Asks the resumable object to perform a unit of work when it is safe to do so.
         * <p/>
         * The thread this method is called from is not defined.
         */
        void postOnResume(@NonNull Runnable runnable);

        /**
         * Asks the resumable to cancel a pending unit of work, if applicable.
         * <p/>
         * The thread this method is called from is not defined.
         */
        void cancelPostOnResume(@NonNull Runnable runnable);
    }
}
