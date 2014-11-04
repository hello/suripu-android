package is.hello.sense.ui.common;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.sense.SenseApplication;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import is.hello.sense.util.ResumeScheduler;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.operators.OperatorConditionalBinding;

public class InjectionFragment extends Fragment implements ObservableContainer, PresenterContainer, ResumeScheduler.Resumable {
    protected ArrayList<Subscription> subscriptions = new ArrayList<>();
    protected ArrayList<Presenter> presenters;

    protected final List<Runnable> onResumeRunnables = new ArrayList<>(); //Synchronize all access to this!
    protected final ResumeScheduler observeScheduler = new ResumeScheduler(this);
    protected static final Func1<Fragment, Boolean> FRAGMENT_VALIDATOR = (fragment) -> fragment.isAdded() && !fragment.getActivity().isFinishing();

    public InjectionFragment() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null && presenters != null) {
            for (Presenter presenter : presenters) {
                if (presenter.isStateRestored())
                    continue;

                Parcelable savedState = savedInstanceState.getParcelable(presenter.getSavedStateKey());
                presenter.onRestoreState(savedState);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (presenters != null) {
            for (Presenter presenter : presenters) {
                Parcelable savedState = presenter.onSaveState();
                outState.putParcelable(presenter.getSavedStateKey(), savedState);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        synchronized(onResumeRunnables) {
            for (Runnable runnable : onResumeRunnables) {
                runnable.run();
            }
            onResumeRunnables.clear();
        }

        if (presenters != null) {
            for (Presenter presenter : presenters) {
                presenter.onContainerResumed();
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (presenters != null) {
            for (Presenter presenter : presenters) {
                presenter.onTrimMemory(level);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (presenters != null) {
            for (Presenter presenter : presenters) {
                presenter.onContainerDestroyed();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        for (Subscription subscription : subscriptions) {
            if (!subscription.isUnsubscribed())
                subscription.unsubscribe();
        }

        subscriptions.clear();
    }

    @Override
    public void postOnResume(@NonNull Runnable runnable) {
        if (isResumed()) {
            runnable.run();
        } else {
            synchronized(onResumeRunnables) {
                onResumeRunnables.add(runnable);
            }
        }
    }

    @Override
    public void cancelPostOnResume(@NonNull Runnable runnable) {
        synchronized(onResumeRunnables) {
            onResumeRunnables.remove(runnable);
        }
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
        return toBind.observeOn(observeScheduler)
                     .lift(new OperatorConditionalBinding<>(this, FRAGMENT_VALIDATOR));
    }

    @Override
    public @NonNull <T> Subscription subscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return track(toSubscribe.subscribe(onNext, onError));
    }

    @Override
    public @NonNull <T> Subscription bindAndSubscribe(@NonNull Observable<T> toSubscribe, Action1<? super T> onNext, Action1<Throwable> onError) {
        return subscribe(bind(toSubscribe), onNext, onError);
    }


    @Override
    public void addPresenter(@NonNull Presenter presenter) {
        getPresenters().add(presenter);
    }

    @Override
    public void removePresenter(@NonNull Presenter presenter) {
        getPresenters().remove(presenter);
    }

    @NonNull
    @Override
    public List<Presenter> getPresenters() {
        if (presenters == null)
            this.presenters = new ArrayList<>();

        return presenters;
    }


    /**
     * Safely pops the fragment from the back stack, propagating a result value
     * and response Intent to the receiver's target fragment.
     * <p/>
     * This method requires a target fragment and the receiver be attached to an activity.
     * @param resultCode    The result code to propagate back.
     * @param response      The result of the fragment.
     * @return  true if the fragment was popped and the target fragment informed; false otherwise.
     */
    protected boolean popFromBackStack(int resultCode, @Nullable Intent response) {
        if (getTargetFragment() != null && getFragmentManager() != null) {
            new Handler().post(() -> getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, response));
            getFragmentManager().popBackStackImmediate();
            return true;
        } else {
            return false;
        }
    }
}
