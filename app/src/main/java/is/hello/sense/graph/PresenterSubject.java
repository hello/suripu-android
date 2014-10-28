package is.hello.sense.graph;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

/**
 * A special subclass of <pre>Subject<T, R></pre> that never stops emitting values.
 * This means that calls to {@see PresenterSubject.onCompleted} do nothing, and that
 * the subject will continue to emit values even after {@see PresenterSubject.onError}
 * has been called. This subject is intended to hold volatile in-memory state for subclasses
 * of the {@see is.hello.sense.graph.presenters.Presenter} class.
 * <p/>
 * PresenterSubject retains, at most, a single value/error in memory at a time.
 * @param <T>   The type of value contained by the subject.
 * @see rx.subjects.Subject
 */
public final class PresenterSubject<T> extends Subject<T, T> {
    private final SubscriptionManager<T> subscriptionManager;

    //region Creation

    public static <T> PresenterSubject<T> create() {
        SubscriptionManager<T> subscriptionManager = new SubscriptionManager<>();
        return new PresenterSubject<>(subscriptionManager);
    }

    private PresenterSubject(@NonNull SubscriptionManager<T> subscriptionManager) {
        super(subscriptionManager);
        this.subscriptionManager = subscriptionManager;
    }

    //endregion


    //region Observer

    /**
     * Calling this method does nothing. Completion is ignored, and subscribers are not notified.
     */
    @Override
    public void onCompleted() {
        // Do nothing.
    }

    @Override
    public void onError(Throwable e) {
        subscriptionManager.error(e);
    }

    @Override
    public void onNext(T value) {
        subscriptionManager.next(value);
    }

    /**
     * Causes the subject to clear its references to
     * the most recent error/value pushed through it.
     * <p/>
     * This method <em>does not</em> inform subscribers
     * of the state being cleared. If you want to clear
     * the state <em>and</em> inform subscribers, you
     * should call {@see onNext} with null. (This requires
     * your subscribers accept null as a valid value.)
     * <p/>
     * Since most volatile state in the app is stored
     * exclusively inside of PresenterSubject objects,
     * calling this method is an ideal response to
     * increasing memory pressure.
     */
    public void forget() {
        subscriptionManager.value = null;
        subscriptionManager.error = null;
    }

    //endregion


    private static class SubscriptionManager<T> implements OnSubscribe<T> {
        private final List<Subscriber<? super T>> subscribers = new ArrayList<>();
        private T value = null;
        private Throwable error = null;

        @Override
        public void call(Subscriber<? super T> subscriber) {
            subscriber.onStart();
            if (!subscriber.isUnsubscribed()) {
                subscriber.add(Subscriptions.create(() -> subscribers.remove(subscriber)));
                subscribers.add(subscriber);

                if (value != null) {
                    subscriber.onNext(value);
                } else if (error != null) {
                    subscriber.onError(error);
                }
            }
        }

        public void next(T value) {
            this.value = value;
            this.error = null;

            for (Subscriber<? super T> subscriber : subscribers) {
                subscriber.onNext(value);
            }
        }

        public void error(Throwable e) {
            this.value = null;
            this.error = e;

            for (Subscriber<? super T> subscriber : subscribers) {
                subscriber.onError(e);
            }
        }


        @Override
        public String toString() {
            return "SubscriptionManager{" +
                    "value=" + value +
                    ", error=" + error +
                    '}';
        }
    }


    @Override
    public String toString() {
        return "PresenterSubject{" +
                "subscriptionManager=" + subscriptionManager +
                '}';
    }
}
