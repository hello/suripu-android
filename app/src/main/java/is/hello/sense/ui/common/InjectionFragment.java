package is.hello.sense.ui.common;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.SenseApplication;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.util.Logger;
import is.hello.sense.util.StateSafeExecutor;
import is.hello.sense.util.StateSafeScheduler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public class InjectionFragment extends SenseFragment implements ObservableContainer, StateSafeExecutor.Resumes {
    protected final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(this);
    protected final StateSafeScheduler observeScheduler = new StateSafeScheduler(stateSafeExecutor);

    protected static final Func1<Fragment, Boolean> FRAGMENT_VALIDATOR = f -> f.isAdded() && !f.getActivity().isFinishing();
    protected final DelegateObservableContainer<Fragment> observableContainer = new DelegateObservableContainer<>(observeScheduler, this, FRAGMENT_VALIDATOR);

    protected final PresenterContainer presenterContainer = new PresenterContainer();

    protected boolean animatorContextFromActivity = false;
    protected @Nullable AnimatorContext animatorContext;


    public InjectionFragment() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (animatorContext == null && activity instanceof AnimatorContext.Scene) {
            this.animatorContext = ((AnimatorContext.Scene) activity).getAnimatorContext();
            this.animatorContextFromActivity = true;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.animatorContext = null;
        this.animatorContextFromActivity = false;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            presenterContainer.onRestoreState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        presenterContainer.onSaveState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        stateSafeExecutor.executePendingForResume();
        presenterContainer.onContainerResumed();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        presenterContainer.onTrimMemory(level);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        observableContainer.clearSubscriptions();
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


    public void addPresenter(@NonNull Presenter presenter) {
        presenterContainer.addPresenter(presenter);
    }


    public @NonNull AnimatorContext getAnimatorContext() {
        if (animatorContext == null) {
            this.animatorContext = new AnimatorContext(getClass().getSimpleName());
            Logger.debug(getClass().getSimpleName(), "Creating animator context");
        }

        return animatorContext;
    }
}
