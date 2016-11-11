package is.hello.sense.flows.home.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.flows.home.ui.views.HomeView;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.HasVoiceInteractor;
import is.hello.sense.ui.widget.SelectorView;

public class HomeFragment extends BacksideTabFragment<HomeView>
        implements SelectorView.OnSelectionChangedListener {

    @Inject
    HasVoiceInteractor hasVoiceInteractor;

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
        addInteractor(hasVoiceInteractor);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(hasVoiceInteractor.hasVoice,
                         presenterView::showVoiceFragment,
                         Functions.LOG_ERROR);
        hasVoiceInteractor.update();

    }

    @Override
    protected void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {

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
