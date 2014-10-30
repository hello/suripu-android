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
 * A wrapper around the Android main thread scheduler that defers work
 * when its containing {@see Resumable} is suspended.
 */
public class ResumeScheduler extends Scheduler {
    private final Resumable target;
    private final Scheduler mainThread = AndroidSchedulers.mainThread();

    public ResumeScheduler(@NonNull Resumable target) {
        this.target = target;
    }

    @Override
    public ResumeWorker createWorker() {
        return new ResumeWorker(target, mainThread.createWorker());
    }


    private static class ResumeWorker extends Worker {
        private final CompositeSubscription compositeSubscription = new CompositeSubscription();
        private final Resumable target;
        private final Worker mainThreadWorker;

        private ResumeWorker(@NonNull Resumable target, @NonNull Worker mainThreadWorker) {
            this.target = target;
            this.mainThreadWorker = mainThreadWorker;
        }

        @Override
        public Subscription schedule(Action0 action) {
            return schedule(action, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            Runnable work = () -> compositeSubscription.add(mainThreadWorker.schedule(action, delayTime, unit));
            Subscription subscription = Subscriptions.create(() -> target.cancelPostOnResume(work));
            compositeSubscription.add(subscription);
            target.postOnResume(work);
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
     * A Fragment or Activity that will defer work until it is resumed.
     */
    public interface Resumable {
        void postOnResume(@NonNull Runnable runnable);
        void cancelPostOnResume(@NonNull Runnable runnable);
    }
}
