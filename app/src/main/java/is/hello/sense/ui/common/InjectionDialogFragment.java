package is.hello.sense.ui.common;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.SenseApplication;
import is.hello.sense.graph.presenters.PresenterContainer;
import is.hello.sense.util.ResumeScheduler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public class InjectionDialogFragment extends DialogFragment implements ObservableContainer {
    protected final ResumeScheduler.Coordinator coordinator = new ResumeScheduler.Coordinator(this::isResumed);
    protected final ResumeScheduler observeScheduler = new ResumeScheduler(coordinator);

    protected static final Func1<DialogFragment, Boolean> FRAGMENT_VALIDATOR = (fragment) -> fragment.isAdded() && !fragment.getActivity().isFinishing();
    protected final DelegateObservableContainer<DialogFragment> observableContainer = new DelegateObservableContainer<>(observeScheduler, this, FRAGMENT_VALIDATOR);

    protected final PresenterContainer presenterContainer = new PresenterContainer();

    public InjectionDialogFragment() {
        SenseApplication.getInstance().inject(this);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            presenterContainer.onRestoreState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        presenterContainer.onSaveState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        coordinator.resume();
        presenterContainer.onContainerResumed();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        presenterContainer.onTrimMemory(level);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        presenterContainer.onContainerDestroyed();
    }


    @Override
    public boolean hasSubscriptions() {
        return observableContainer.hasSubscriptions();
    }

    @Override
    @NonNull
    public Subscription track(@NonNull Subscription subscription) {
        return observableContainer.track(subscription);
    }

    @Override
    @NonNull
    public <T> Observable<T> bind(@NonNull Observable<T> toBind) {
        return observableContainer.bind(toBind);
    }

    @Override
    @NonNull
    public <T> Subscription subscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return observableContainer.subscribe(toSubscribe, onNext, onError);
    }

    @Override
    @NonNull
    public <T> Subscription bindAndSubscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return observableContainer.bindAndSubscribe(toSubscribe, onNext, onError);
    }
}
