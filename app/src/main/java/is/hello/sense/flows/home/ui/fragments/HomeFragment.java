package is.hello.sense.flows.home.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;


import javax.inject.Inject;

import is.hello.sense.flows.home.ui.views.HomeView;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.ui.widget.SelectorView;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class HomeFragment extends BacksideTabFragment<HomeView>
        implements SelectorView.OnSelectionChangedListener {

    @Inject
    PreferencesInteractor preferencesInteractor;
    @Inject
    DevicesInteractor devicesInteractor;

    private Subscription userFeaturesSubscription = Subscriptions.empty();

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new HomeView(getActivity(),
                                         getChildFragmentManager(),
                                         getAnimatorContext(),
                                         stateSafeExecutor,
                                         this);
        }
    }

    @Override
    public final void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (presenterView != null && presenterView.isShowingViews()) {
            final Fragment fragment = presenterView.getCurrentFragment();
            if (fragment != null) {
                fragment.setUserVisibleHint(isVisibleToUser);
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(preferencesInteractor);
        addInteractor(devicesInteractor);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(preferencesInteractor.observableBoolean(PreferencesInteractor.HAS_VOICE, false),
                         presenterView::showVoiceFragment,
                         Functions.LOG_ERROR);

        if (!preferencesInteractor.contains(PreferencesInteractor.HAS_VOICE)) {
            userFeaturesSubscription.unsubscribe();
            userFeaturesSubscription = bind(devicesInteractor.devices)
                    .subscribe((devices) -> preferencesInteractor.setDevice(devices.getSense()),
                               Functions.LOG_ERROR);
            devicesInteractor.update();
        }

    }

    @Override
    protected void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {

    }

    @Override
    protected void onRelease() {
        super.onRelease();
        userFeaturesSubscription.unsubscribe();
        userFeaturesSubscription = Subscriptions.empty();
    }

    @Override
    public void onSelectionChanged(final int newSelectionIndex) {
        this.presenterView.setPagerItem(newSelectionIndex);

    }

    public Fragment getCurrentFragment() {
        return getChildFragmentManager().findFragmentByTag("android:switcher:" + presenterView.getPagerId()
                                                                   + ":" + presenterView.currentPagerItem());
    }

}
