package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.interactors.Interactor;
import is.hello.sense.interactors.InteractorContainer;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.DelegateObservableContainer;
import is.hello.sense.ui.common.ObservableContainer;
import is.hello.sense.ui.common.StateSaveable;
import is.hello.sense.util.Logger;
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
 *
 * @param <S> indicates the generic type of {@link BaseOutput} that the Presenter will expect its view to implement
 */
public abstract class BasePresenter<S extends BaseOutput>
        implements
        Presenter,
        PresenterOutputLifecycle<S>,
        ObservableContainer,
        StateSafeExecutor.Resumes,
        StateSaveable {

    protected static final Func1<BasePresenter, Boolean> VALIDATOR = BasePresenter::canObservableEmit; //todo add more

    protected final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(this);
    protected final StateSafeScheduler observeScheduler = new StateSafeScheduler(stateSafeExecutor);
    protected final DelegateObservableContainer<BasePresenter> observableContainer = new DelegateObservableContainer<>(observeScheduler, this, VALIDATOR);

    //region Presenter
    protected S view;
    protected final InteractorContainer interactorContainer = new InteractorContainer();


    /**
     * Currently set during onAttach and released in onDetach of fragment lifecycle
     * @param view Bind reference to BaseOutput
     */
    public void setView(final S view) {
        this.view = view;
    }

    @CallSuper
    public void onDestroyView() {
        observableContainer.clearSubscriptions();
        interactorContainer.onContainerDestroyed();
    }

    /**
     * Intended to be used when {@link Interactor} values want to be released
     */
    public abstract void onDetach();
    //endregion

    private boolean stateRestored = false;

    //region StateSaveable
    @CallSuper
    @Override
    public boolean isStateRestored() {
        return stateRestored;
    }

    @CallSuper
    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        logEvent("onRestoreState(" + savedState + ")");
        stateRestored = true;
        interactorContainer.onRestoreState(savedState);
    }

    @Override
    @Nullable
    public Bundle onSaveState() {
        logEvent("onSaveState()");
        return null;
    }

    @Override
    @NonNull
    public String getSavedStateKey() {
        return getClass().getSimpleName() + "#instanceState";
    }

    //endregion

    //region Logging

    protected void logEvent(@NonNull final String event) {
        Logger.debug("scopedPresenters", getClass().getSimpleName() + ": " + event);
    }


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
        return view != null && view.canObservableEmit();
    }
    //endregion


    //region Interactor Container
    @CallSuper
    public void onResume() {
        stateSafeExecutor.executePendingForResume();
        interactorContainer.onContainerResumed();
    }

    public void onCreate(@Nullable final Bundle savedInstanceState) {
    }

    /**
     * Should bind {@link is.hello.sense.interactors.ValueInteractor} here.
     */
    public void onViewCreated() {

    }

    public void onPause() {

    }

    public void onStop() {

    }

    @CallSuper
    public void onSaveInteractorState(@NonNull final Bundle outState) {
        interactorContainer.onSaveState(outState);
    }

    @CallSuper
    public void onTrimMemory(final int level) {
        interactorContainer.onTrimMemory(level);
    }

    public void addInteractor(@NonNull final Interactor interactor) {
        interactorContainer.addInteractor(interactor);
    }

    public void execute(@NonNull final Runnable runnable) {
        stateSafeExecutor.execute(runnable);
    }

    public Runnable bind(@NonNull final Runnable runnable) {
        return stateSafeExecutor.bind(runnable);
    }
    //endregion

    private Boolean canObservableEmit() {
        return view != null && view.canObservableEmit();
    }

    protected void showBlockingActivity(@StringRes final int titleRes) {
        execute(() -> view.showBlockingActivity(titleRes));
    }

    protected void hideBlockingActivity(final boolean success, @NonNull final Runnable onComplete) {
        execute(() -> view.hideBlockingActivity(success, bind(onComplete)));
    }

    protected void hideBlockingActivity(@StringRes final int messageRes, @NonNull final Runnable onComplete) {
        execute(() -> view.hideBlockingActivity(messageRes, bind(onComplete)));
    }

}