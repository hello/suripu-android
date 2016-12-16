package is.hello.sense.flows.home.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.flows.home.ui.views.SoundsView;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SleepSoundsInteractor;
import is.hello.sense.ui.common.SubFragment;
import is.hello.sense.ui.widget.SelectorView.OnSelectionChangedListener;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

@Deprecated
public class SoundsFragment extends BacksideTabFragment<SoundsView>
        implements OnSelectionChangedListener,
        SwipeRefreshLayout.OnRefreshListener {

    @Inject
    PreferencesInteractor preferencesInteractor;
    @Inject
    SleepSoundsInteractor sleepSoundsInteractor;

    @NonNull
    private Subscription sleepSoundsSubscription = Subscriptions.empty();

    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new SoundsView(getActivity(),
                                           getChildFragmentManager(),
                                           getAnimatorContext(),
                                           stateSafeExecutor);
        }
    }

    @Override
    public final void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (presenterView != null && presenterView.isShowingViews()) {
            final SubFragment fragment = presenterView.getCurrentSubFragment(getChildFragmentManager());
            if (fragment != null) {
                // This is what stops SleepSoundsFragment from polling when the fragment changes.
                fragment.setUserVisibleHint(isVisibleToUser);
            }
        }
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(sleepSoundsInteractor);
        setHasOptionsMenu(true);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(preferencesInteractor.observableBoolean(PreferencesInteractor.HAS_SOUNDS, false),
                         presenterView::refreshView,
                         this::presentError);
        presenterView.setSubNavSelectorOnSelectionChangedListener(this);
        presenterView.setSwipeRefreshLayoutOnRefreshListener(this);
        onUpdate();
    }

    @Override
    protected final void onSwipeInteractionDidFinish() {

    }

    @Override
    public final void onUpdate() {
        presenterView.updated();

        sleepSoundsSubscription.unsubscribe();
        sleepSoundsSubscription =
                bind(sleepSoundsInteractor.hasSensePaired()).subscribe(this::bind,
                                                                       this::presentError);
    }

    @Override
    public final void onSelectionChanged(final int newSelectionIndex) {
        presenterView.setPagerItem(newSelectionIndex);
    }

    public final void bind(final boolean show) {
        preferencesInteractor.edit().putBoolean(PreferencesInteractor.HAS_SOUNDS, true).apply();
        presenterView.refreshView(true);

    }

    public final void presentError(@NonNull final Throwable error) {
        preferencesInteractor.edit().putBoolean(PreferencesInteractor.HAS_SOUNDS, false).apply();
        presenterView.refreshView(false);
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        sleepSoundsSubscription.unsubscribe();
        sleepSoundsSubscription = Subscriptions.empty();

    }

    @Override
    public final void onRefresh() {
        presenterView.refreshed(getChildFragmentManager());
        presenterView.refreshView(preferencesInteractor.getBoolean(PreferencesInteractor.HAS_SOUNDS, false));
    }

}
