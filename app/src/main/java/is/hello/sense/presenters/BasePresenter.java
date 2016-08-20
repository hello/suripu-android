package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.interactors.Interactor;
import is.hello.sense.interactors.InteractorContainer;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.DelegateObservableContainer;
import is.hello.sense.ui.common.ObservableContainer;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.StateSafeExecutor;
import is.hello.sense.util.StateSafeScheduler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public abstract class BasePresenter<S extends BaseOutput>
        implements Presenter, ObservableContainer, StateSafeExecutor.Resumes, PresenterOutputLifecycle<S> {

    protected S viewOutput;
    protected static final Func1<BasePresenter, Boolean> FRAGMENT_VALIDATOR = BasePresenter::isResumed; //todo add more
    protected final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(this);
    protected final StateSafeScheduler observeScheduler = new StateSafeScheduler(stateSafeExecutor);
    protected final DelegateObservableContainer<BasePresenter> observableContainer = new DelegateObservableContainer<>(observeScheduler, this, FRAGMENT_VALIDATOR);
    protected final InteractorContainer interactorContainer = new InteractorContainer();

    // region ObservableContainer
    @NonNull
    @Override
    public <T> Observable<T> bind(@NonNull final Observable<T> toBind) {
        return observableContainer.bind(toBind);
    }

    @Override
    public boolean hasSubscriptions() {
        return observableContainer.hasSubscriptions();
    }

    @NonNull
    @Override
    public Subscription track(@NonNull final Subscription subscription) {
        return observableContainer.track(subscription);
    }

    @NonNull
    @Override
    public <T> Subscription subscribe(@NonNull final Observable<T> toSubscribe,
                                      @NonNull final Action1<? super T> onNext,
                                      @NonNull final Action1<Throwable> onError) {
        return observableContainer.subscribe(toSubscribe, onNext, onError);
    }

    @NonNull
    @Override
    public <T> Subscription bindAndSubscribe(@NonNull final Observable<T> toSubscribe,
                                             @NonNull final Action1<? super T> onNext,
                                             @NonNull final Action1<Throwable> onError) {
        return observableContainer.bindAndSubscribe(toSubscribe, onNext, onError);
    }
    // endregion

    //region StateSafeExecutor.Resumes
    @Override
    public boolean isResumed() {
        return viewOutput != null && viewOutput.isResumed();
    }
    //endregion

    //region PresenterOutputLifecycle

    public void setView(final S view) {
        this.viewOutput = view;
    }

    @Override
    public void onDestroyView() {
        this.viewOutput = null;
    }

    @Override
    public void onDestroy() {

    }
    //endregion
}
