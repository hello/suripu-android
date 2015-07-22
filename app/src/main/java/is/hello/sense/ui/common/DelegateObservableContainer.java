package is.hello.sense.ui.common;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import is.hello.buruberi.util.Rx;
import is.hello.sense.graph.PresenterSubject;
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

    public DelegateObservableContainer(@NonNull Scheduler scheduler,
                                       @NonNull Target bindTarget,
                                       @NonNull Func1<Target, Boolean> bindPredicate) {
        this.scheduler = scheduler;
        this.bindTarget = bindTarget;
        this.bindPredicate = bindPredicate;
    }

    public void clearSubscriptions() {
        for (Subscription subscription : subscriptions) {
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
    public @NonNull Subscription track(@NonNull Subscription subscription) {
        subscriptions.add(subscription);
        return subscription;
    }

    @Override
    public @NonNull <T> Observable<T> bind(@NonNull Observable<T> toBind) {
        Observable<T> bound;
        if (toBind instanceof PresenterSubject) {
            bound = toBind.lift(new Rx.OperatorUnbufferedObserveOn<>(scheduler));
        } else {
            bound = toBind.observeOn(scheduler);
        }
        return bound.lift(new Rx.OperatorConditionalBinding<>(bindTarget, bindPredicate));
    }

    @Override
    public @NonNull <T> Subscription subscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return track(toSubscribe.unsafeSubscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {
                unsubscribe();
            }

            @Override
            public void onError(Throwable e) {
                if (isUnsubscribed())
                    return;

                try {
                    onError.call(e);
                } catch (Throwable actionError) {
                    Logger.error(bindTarget.getClass().getSimpleName(), "onError handler threw an exception, crashing", e);
                    throw actionError;
                }
            }

            @Override
            public void onNext(T t) {
                if (isUnsubscribed())
                    return;

                onNext.call(t);
            }
        }));
    }

    @Override
    public @NonNull <T> Subscription bindAndSubscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return subscribe(bind(toSubscribe), onNext, onError);
    }
}
