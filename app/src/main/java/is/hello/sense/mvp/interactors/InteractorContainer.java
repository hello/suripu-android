package is.hello.sense.mvp.interactors;

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.SenseApplication;
import is.hello.sense.interactors.Interactor;
import is.hello.sense.ui.common.DelegateObservableContainer;
import is.hello.sense.ui.common.ObservableContainer;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.util.StateSafeExecutor;
import is.hello.sense.util.StateSafeScheduler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;


/**
 * Contains one or more child presenter objects, allowing a containing object
 * to send lifecycle events to all of its interactors with a single method call.
 *
 * @see Interactor
 */
public abstract class InteractorContainer
        implements ObservableContainer {
    private static final Func1<SenseFragment, Boolean> FRAGMENT_VALIDATOR = f -> f.isAdded() && !f.getActivity().isFinishing();
    private StateSafeExecutor stateSafeExecutor;
    private StateSafeScheduler observeScheduler;
    private DelegateObservableContainer<SenseFragment> observableContainer;

    private final List<SenseInteractor> interactors = new ArrayList<>();

    public InteractorContainer(@NonNull final SenseFragment senseFragment) {
        this.stateSafeExecutor = new StateSafeExecutor(senseFragment);
        this.observeScheduler = new StateSafeScheduler(stateSafeExecutor);
        this.observableContainer = new DelegateObservableContainer<>(observeScheduler, senseFragment, FRAGMENT_VALIDATOR);
        SenseApplication.getInstance().inject(this);
        addInteractors();
    }


    //region ObserverContainer

    @Override
    public boolean hasSubscriptions() {
        return this.observableContainer.hasSubscriptions();
    }

    @Override
    @NonNull
    public Subscription track(@NonNull final Subscription subscription) {
        return this.observableContainer.track(subscription);
    }

    @Override
    @NonNull
    public <T> Observable<T> bind(@NonNull final Observable<T> toBind) {
        return this.observableContainer.bind(toBind);
    }

    @Override
    @NonNull
    public <T> Subscription subscribe(@NonNull final Observable<T> toSubscribe,
                                      @NonNull final Action1<? super T> onNext,
                                      @NonNull final Action1<Throwable> onError) {
        return this.observableContainer.subscribe(toSubscribe, onNext, onError);
    }

    @Override
    @NonNull
    public <T> Subscription bindAndSubscribe(@NonNull final Observable<T> toSubscribe,
                                             @NonNull final Action1<? super T> onNext,
                                             @NonNull final Action1<Throwable> onError) {
        return this.observableContainer.bindAndSubscribe(toSubscribe, onNext, onError);
    }
    //endregion

    //region methods

    /**
     * /**
     *
     * @see Interactor#onContainerResumed()
     */
    public void onContainerResumed() {
        for (final Interactor interactor : this.interactors) {
            interactor.onContainerResumed();
        }
    }

    /**
     * @see Interactor#onTrimMemory(int)
     */
    public void onTrimMemory(final int level) {
        for (final Interactor interactor : this.interactors) {
            interactor.onTrimMemory(level);
        }
    }

    /**
     * @see Interactor#onRestoreState(android.os.Bundle)
     */
    public void onRestoreState(@NonNull final Bundle inState) {
        for (final Interactor interactor : this.interactors) {
            if (interactor.isStateRestored()) {
                continue;
            }

            final Bundle savedState = inState.getParcelable(interactor.getSavedStateKey());
            if (savedState != null) {
                interactor.onRestoreState(savedState);
            }
        }
    }

    /**
     * @see Interactor#onSaveState()
     */
    public void onSaveState(final Bundle outState) {
        for (final Interactor interactor : this.interactors) {
            final Bundle savedState = interactor.onSaveState();
            if (savedState != null) {
                outState.putParcelable(interactor.getSavedStateKey(), savedState);
            }
        }
    }

    public final void onDestroyView() {
        this.observableContainer.clearSubscriptions();
    }

    public final void onResume() {
        this.stateSafeExecutor.executePendingForResume();
    }

    public final void onDetach() {
        this.stateSafeExecutor = null;
        this.observeScheduler = null;
        this.observableContainer = null;
    }

    /**
     * Add a child interactor to the container.
     */
    public void addInteractor(@NonNull final SenseInteractor interactor) {
        this.interactors.add(interactor);
    }


    public abstract void addInteractors();

    public List<SenseInteractor> getInteractors() {
        return interactors;
    }

    //endregion

    public static class EmptyInteractorContainer extends InteractorContainer {
        public EmptyInteractorContainer(@NonNull final SenseFragment senseFragment) {
            super(senseFragment);
        }

        @Override
        public void addInteractors() {

        }
    }

}
