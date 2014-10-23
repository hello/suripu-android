package is.hello.sense.ui.common;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.SenseApplication;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import rx.Observable;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;

public class InjectionFragment extends Fragment implements ObservableContainer, PresenterContainer {
    protected ArrayList<Subscription> subscriptions = new ArrayList<>();
    protected ArrayList<Presenter> presenters;

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
        return AndroidObservable.bindFragment(this, toBind);
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
}
