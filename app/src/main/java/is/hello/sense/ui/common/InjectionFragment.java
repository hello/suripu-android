package is.hello.sense.ui.common;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.SenseApplication;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.graph.presenters.PresenterContainer;
import rx.Subscription;

public class InjectionFragment extends Fragment implements SubscriptionTracker, PresenterContainer {
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
