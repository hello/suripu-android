package is.hello.sense.presenters;

import android.support.annotation.NonNull;

import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.DelegateObservableContainer;
import is.hello.sense.ui.common.ObservableContainer;
import is.hello.sense.util.StateSafeExecutor;
import is.hello.sense.util.StateSafeScheduler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Contains {@link ObservableContainer},
 * {@link StateSafeExecutor},
 * {@link StateSafeScheduler},
 * {@link DelegateObservableContainer}
 * @param <T> indicates the generic type of {@link BaseOutput} that the Presenter will expect its view to implement
 */
public abstract class BasePresenter<T extends BaseOutput>
        implements
        Presenter,
        PresenterOutputLifecycle<T>,
        ObservableContainer,
        StateSafeExecutor.Resumes {

    protected static final Func1<BasePresenter, Boolean> VALIDATOR = BasePresenter::isResumed; //todo add more
    protected final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(this);
    protected final StateSafeScheduler observeScheduler = new StateSafeScheduler(stateSafeExecutor);
    protected final DelegateObservableContainer<BasePresenter> observableContainer = new DelegateObservableContainer<>(observeScheduler, this, VALIDATOR);

    //region Presenter
    protected T view;

    public void setView(final T view){
        this.view = view;
    }

    public void onDestroyView(){
        this.view = null;
    }

    public abstract void onDestroy();
    //endregion

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
        return view != null && view.isResumed();
    }
    //endregion

}