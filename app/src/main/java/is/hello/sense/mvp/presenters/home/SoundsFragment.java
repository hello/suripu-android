package is.hello.sense.mvp.presenters.home;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.SleepSoundsStateDevice;
import is.hello.sense.interactors.SleepSoundsInteractor;
import is.hello.sense.mvp.view.home.SoundsView;
import is.hello.sense.ui.common.SubFragment;
import is.hello.sense.ui.widget.SelectorView.OnSelectionChangedListener;



public class SoundsFragment extends BacksideTabFragment<SoundsView> implements OnSelectionChangedListener, SwipeRefreshLayout.OnRefreshListener {

    @Inject
    SleepSoundsInteractor sleepSoundsInteractor;

    @Override
    public SoundsView getPresenterView() {
        if (presenterView == null) {
            return new SoundsView(getActivity(), getAnimatorContext(), stateSafeExecutor);
        }
        return presenterView;
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            sleepSoundsInteractor.update();
            bindAndSubscribe(sleepSoundsInteractor.sub, this::bind, this::presentError);
        }
        if (presenterView != null && presenterView.isShowingViews()) {

            final SubFragment fragment = presenterView.getCurrentSubFragment(getChildFragmentManager());
            if (fragment != null) {
                // This is what stops SleepSoundsFragment from polling when the fragment changes.
                fragment.setUserVisibleHint(isVisibleToUser);
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(sleepSoundsInteractor);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putBoolean(SoundsView.ARG_HAS_NAVBAR, presenterView.isSubNavBarVisible());
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.setSubNavSelectorOnSelectionChangedListener(this);
        presenterView.setSwipeRefreshLayoutOnRefreshListener(this);
        presenterView.setAdapter(getChildFragmentManager(), savedInstanceState);
        onUpdate();
    }

    @Override
    public void onResume() {
        super.onResume();
        sleepSoundsInteractor.update();
    }

    @Override
    protected void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {
        presenterView.updated();
    }

    @Override
    public void onSelectionChanged(final int newSelectionIndex) {
        presenterView.setPagerItem(newSelectionIndex);
    }

    public void bind(@NonNull final SleepSoundsStateDevice stateDevice) {
        final Devices devices = stateDevice.getDevices();
        final SleepSoundsState state = stateDevice.getSleepSoundsState();
        boolean show = false;
        if (devices != null && state != null) {
            if (devices.getSense() != null && state.getSounds() != null) {
                show = true;
            }
        }
        presenterView.refreshView(show);
    }

    public void presentError(@NonNull final Throwable error) {
        presenterView.refreshView(false);
    }


    @Override
    public void onRefresh() {
        sleepSoundsInteractor.update();
        presenterView.refreshed(getChildFragmentManager());
    }

}
