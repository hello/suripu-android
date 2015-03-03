package is.hello.sense.util;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * A scheduler that wraps a work coordinator and another scheduler, and uses these objects
 * to ensure that work is only run within safe conditions. Used to ensure that no work is
 * done by activities and fragments that are paused or destroyed.
 *
 * @see is.hello.sense.ui.activities.SenseActivity
 * @see is.hello.sense.ui.common.InjectionFragment
 */
public class ResumeScheduler extends Scheduler {
    private final Coordinator targetCoordinator;
    private final Scheduler targetScheduler;

    public ResumeScheduler(@NonNull Coordinator targetCoordinator, @NonNull Scheduler targetScheduler) {
        this.targetCoordinator = targetCoordinator;
        this.targetScheduler = targetScheduler;
    }

    /**
     * Creates a ResumeScheduler targeting the main thread.
     */
    public ResumeScheduler(@NonNull Coordinator targetCoordinator) {
        this(targetCoordinator, AndroidSchedulers.mainThread());
    }

    @Override
    public ResumeWorker createWorker() {
        return new ResumeWorker(targetCoordinator, targetScheduler.createWorker());
    }


    private static class ResumeWorker extends Worker {
        private final CompositeSubscription compositeSubscription = new CompositeSubscription();
        private final Coordinator targetCoordinator;
        private final Worker targetWorker;

        private ResumeWorker(@NonNull Coordinator targetCoordinator, @NonNull Worker targetWorker) {
            this.targetCoordinator = targetCoordinator;
            this.targetWorker = targetWorker;
        }

        @Override
        public Subscription schedule(Action0 action) {
            return schedule(action, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            Runnable work = () -> compositeSubscription.add(targetWorker.schedule(action, delayTime, unit));
            Subscription subscription = Subscriptions.create(() -> targetCoordinator.cancelPostOnResume(work));
            compositeSubscription.add(subscription);
            targetCoordinator.postOnResume(work);
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
     * Coordinates work between a resume scheduler, and a containing object
     * which has a paused state in which some work is deemed unsafe.
     */
    public static class Coordinator {
        private final Func0<Boolean> isResumed;
        private final List<Runnable> actions = new ArrayList<>(); //Synchronize all access to this!

        public Coordinator(@NonNull Func0<Boolean> isResumed) {
            this.isResumed = isResumed;
        }

        public void resume() {
            synchronized(actions) {
                for (Runnable runnable : actions) {
                    runnable.run();
                }
                actions.clear();
            }
        }

        /**
         * Asks the resumable object to perform a unit of work when it is safe to do so.
         * <p/>
         * The thread this method is called from is not defined.
         */
        public void postOnResume(@NonNull Runnable runnable) {
            if (isResumed.call()) {
                runnable.run();
            } else {
                synchronized(actions) {
                    actions.add(runnable);
                }
            }
        }

        /**
         * Asks the resumable to cancel a pending unit of work, if applicable.
         * <p/>
         * The thread this method is called from is not defined.
         */
        public void cancelPostOnResume(@NonNull Runnable runnable) {
            synchronized(actions) {
                actions.remove(runnable);
            }
        }

        /**
         * Binds a given runnable to always be run through this coordinator.
         */
        public @NonNull Runnable bind(@NonNull Runnable runnable) {
            return () -> postOnResume(runnable);
        }
    }
}
