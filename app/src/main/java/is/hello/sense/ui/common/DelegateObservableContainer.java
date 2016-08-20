package is.hello.sense.ui.common;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import is.hello.buruberi.util.Rx;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public final class DelegateObservableContainer<Target> implements ObservableContainer {
    private final ArrayList<Subscription> subscriptions = new ArrayList<>();
    private final Scheduler scheduler;
    private final Target bindTarget;
    private final Func1<Target, Boolean> bindPredicate;

    public DelegateObservableContainer(@NonNull final Scheduler scheduler,
                                       @NonNull final Target bindTarget,
                                       @NonNull final Func1<Target, Boolean> bindPredicate) {
        this.scheduler = scheduler;
        this.bindTarget = bindTarget;
        this.bindPredicate = bindPredicate;
    }

    public void clearSubscriptions() {
        for (final Subscription subscription : subscriptions) {
            if (!subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
        }

        subscriptions.clear();
    }

    @Override
    public boolean hasSubscriptions() {
        return !subscriptions.isEmpty();
    }

    @Override
    public
    @NonNull
    Subscription track(@NonNull final Subscription subscription) {
        subscriptions.add(subscription);
        return subscription;
    }

    @Override
    public
    @NonNull
    <T> Observable<T> bind(@NonNull final Observable<T> toBind) {
        final Observable<T> bound;
        if (toBind instanceof InteractorSubject) {
            bound = toBind.lift(new Rx.OperatorUnbufferedObserveOn<>(scheduler));
        } else {
            bound = toBind.observeOn(scheduler);
        }
        return bound.lift(new Rx.OperatorConditionalBinding<>(bindTarget, bindPredicate));
    }

    @Override
    public
    @NonNull
    <T> Subscription subscribe(@NonNull final Observable<T> toSubscribe,
                               @NonNull final Action1<? super T> onNext,
                               @NonNull final Action1<Throwable> onError) {
        return track(toSubscribe.unsafeSubscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {
                unsubscribe();
            }

            @Override
            public void onError(final Throwable e) {
                if (isUnsubscribed()) {
                    return;
                }

                try {
                    onError.call(e);
                } catch (final Throwable actionError) {
                    Logger.error(bindTarget.getClass().getSimpleName(), "onError handler threw an exception, crashing", e);
                    throw actionError;
                }
            }

            @Override
            public void onNext(final T t) {
                if (isUnsubscribed()) {
                    return;
                }

                onNext.call(t);
            }
        }));
    }

    @Override
    public
    @NonNull
    <T> Subscription bindAndSubscribe(@NonNull final Observable<T> toSubscribe,
                                      @NonNull final Action1<? super T> onNext,
                                      @NonNull final Action1<Throwable> onError) {
        return subscribe(bind(toSubscribe), onNext, onError);
    }
}
