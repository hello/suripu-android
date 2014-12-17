package is.hello.sense.graph;

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;

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


    @Override
    public boolean hasObservers() {
        return !subscriptionManager.subscribers.isEmpty();
    }

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


    /**
     * Serialize the current value of the presenter subject into a Bundle.
     * @return true if the value was serialized; false otherwise.
     */
    public boolean saveState(@NonNull String key, @NonNull Bundle outState) {
        T value = subscriptionManager.value;
        if (value != null && value instanceof Serializable) {
            outState.putSerializable(key, (Serializable) value);
            return true;
        } else {
            return false;
        }
    }

    //endregion


    private static class SubscriptionManager<T> implements OnSubscribe<T> {
        /*
            Have to use a concurrent collection to back the subscription manager,
            multiple threads can be creating and destroying subscriptions.

            ConcurrentLinkedQueue should generally result in behavior consistent
            with the Subject classes shipped with RxJava. There is a possible
            race condition where a subscription can be removed and still be
            notified due to unsubscription operations being asynchronous, all
            of the RxJava Subjects display this same behavior, so it's better
            to be consistent with the library than be 'correct'.

            UPDATE this comment if the type of `subscribers` changes.
        */
        private final ConcurrentLinkedQueue<Subscriber<? super T>> subscribers = new ConcurrentLinkedQueue<>();

        private T value = null;
        private Throwable error = null;

        @Override
        public void call(Subscriber<? super T> subscriber) {
            subscriber.onStart();
            subscriber.add(Subscriptions.create(() -> subscribers.remove(subscriber)));
            if (!subscriber.isUnsubscribed()) {
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
