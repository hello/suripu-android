package is.hello.sense.mvp.presenters;

import android.app.Fragment;
import android.support.annotation.NonNull;

import is.hello.sense.ui.common.DelegateObservableContainer;
import is.hello.sense.ui.common.ObservableContainer;
import is.hello.sense.util.StateSafeExecutor;
import is.hello.sense.util.StateSafeScheduler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public abstract class ObserverFragment extends ScopedInjectionFragment implements ObservableContainer {
    protected final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(this);
    protected static final Func1<Fragment, Boolean> FRAGMENT_VALIDATOR = f -> f.isAdded() && !f.getActivity().isFinishing();
    protected final StateSafeScheduler observeScheduler = new StateSafeScheduler(stateSafeExecutor);

    protected final DelegateObservableContainer<Fragment> observableContainer = new DelegateObservableContainer<>(observeScheduler, this, FRAGMENT_VALIDATOR);


    @Override
    public boolean hasSubscriptions() {
        return observableContainer.hasSubscriptions();
    }

    @Override
    @NonNull
    public Subscription track(@NonNull final Subscription subscription) {
        return observableContainer.track(subscription);
    }

    @Override
    @NonNull
    public <T> Observable<T> bind(@NonNull final Observable<T> toBind) {
        return observableContainer.bind(toBind);
    }

    @Override
    @NonNull
    public <T> Subscription subscribe(@NonNull final Observable<T> toSubscribe,
                                      @NonNull final Action1<? super T> onNext,
                                      @NonNull final Action1<Throwable> onError) {
        return observableContainer.subscribe(toSubscribe, onNext, onError);
    }

    @Override
    @NonNull
    public <T> Subscription bindAndSubscribe(@NonNull final Observable<T> toSubscribe,
                                             @NonNull final Action1<? super T> onNext,
                                             @NonNull final Action1<Throwable> onError) {
        return observableContainer.bindAndSubscribe(toSubscribe, onNext, onError);
    }


}
