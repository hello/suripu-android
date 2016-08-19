package is.hello.sense.ui.fragments.sounds;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.SleepSoundsStateDevice;
import is.hello.sense.interactors.SleepSoundsInteractor;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.common.SubFragment;
import is.hello.sense.ui.fragments.BacksideTabFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.SelectorView.OnSelectionChangedListener;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.animators.MultiAnimator.animatorFor;


public class SoundsFragment extends BacksideTabFragment implements OnSelectionChangedListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String ARG_HAS_NAVBAR = SoundsFragment.class.getName() + ".ARG_HAS_NAVBAR";

    private ProgressBar initialActivityIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SelectorView subNavSelector;
    private ExtendedViewPager pager;
    private StaticFragmentAdapter adapter;

    @Inject
    SleepSoundsInteractor sleepSoundsPresenter;

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            sleepSoundsPresenter.update();
            bindAndSubscribe(sleepSoundsPresenter.sub, this::bind, this::presentError);
        }
        if (pager != null && adapter != null && pager.getChildCount() == adapter.getCount()) {

            final SubFragment fragment = getSubFragment(pager.getCurrentItem());
            if (fragment != null) {
                // This is what stops SleepSoundsFragment from polling when the fragment changes.
                fragment.setUserVisibleHint(isVisibleToUser);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        interactorContainer.addPresenter(sleepSoundsPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sounds, container, false);
        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_sounds_refresh_container);
        swipeRefreshLayout.setEnabled(true);
        swipeRefreshLayout.setOnRefreshListener(this);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        this.initialActivityIndicator = (ProgressBar) view.findViewById(R.id.fragment_sounds_loading);
        this.pager = (ExtendedViewPager) view.findViewById(R.id.fragment_sounds_scrollview);
        this.subNavSelector = (SelectorView) view.findViewById(R.id.fragment_sounds_sub_nav);
        pager.setScrollingEnabled(false);
        subNavSelector.setButtonLayoutParams(new SelectorView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        subNavSelector.setBackground(new TabsBackgroundDrawable(getResources(),
                                                                TabsBackgroundDrawable.Style.SUBNAV));
        subNavSelector.addOption(R.string.alarm_subnavbar_alarm_list, false);
        subNavSelector.addOption(R.string.alarm_subnavbar_sounds_list, false);
        subNavSelector.setOnSelectionChangedListener(this);
        subNavSelector.setTranslationY(0);

        this.adapter = new StaticFragmentAdapter(getChildFragmentManager(),
                                                 new StaticFragmentAdapter.Item(SmartAlarmListFragment.class, getString(R.string.alarm_subnavbar_alarm_list)),
                                                 new StaticFragmentAdapter.Item(SleepSoundsFragment.class, getString(R.string.alarm_subnavbar_sounds_list)));

        pager.setAdapter(adapter);
        pager.setFadePageTransformer(true);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_HAS_NAVBAR)) {
                refreshView(savedInstanceState.getBoolean(ARG_HAS_NAVBAR));
            }
        } else {
            refreshView(false);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        initialActivityIndicator = null;
        swipeRefreshLayout = null;
        subNavSelector = null;
        pager = null;
        adapter = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        boolean isVisible = false;
        if (subNavSelector != null && subNavSelector.getVisibility() == View.VISIBLE) {
            isVisible = true;
        }
        outState.putBoolean(ARG_HAS_NAVBAR, isVisible);

    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subNavSelector.setSelectedIndex(pager.getCurrentItem());
        swipeRefreshLayout.setRefreshing(true);
        onUpdate();
    }

    @Override
    public void onResume() {
        super.onResume();
        sleepSoundsPresenter.update();
        subNavSelector.setSelectedIndex(pager.getCurrentItem());
    }

    @Override
    protected void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {
        initialActivityIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onSelectionChanged(final int newSelectionIndex) {
        pager.setCurrentItem(newSelectionIndex);

    }

    private SubFragment getSubFragment(int position) {
        final long itemId = adapter.getItemId(position);
        final String tag = "android:switcher:" + pager.getId() + ":" + itemId;
        return (SubFragment) getChildFragmentManager().findFragmentByTag(tag);
    }

    public void bind(@NonNull SleepSoundsStateDevice stateDevice) {
        final Devices devices = stateDevice.getDevices();
        final SleepSoundsState state = stateDevice.getSleepSoundsState();
        boolean show = false;
        if (devices != null && state != null) {
            if (devices.getSense() != null && state.getSounds() != null) {
                show = true;
            }
        }
        refreshView(show);
    }

    public void presentError(@NonNull Throwable error) {
        refreshView(false);
        //todo check again?
    }


    private void refreshView(boolean show) {
        if (subNavSelector == null || swipeRefreshLayout == null) {
            return;
        }
        swipeRefreshLayout.setRefreshing(false);
        if (show && subNavSelector.getVisibility() == View.GONE) {
            transitionInSubNavBar();
            adapter.setOverrideCount(-1);
            adapter.notifyDataSetChanged();
        } else if (!show && subNavSelector.getVisibility() == View.VISIBLE) {
            transitionOutSubNavBar();
            adapter.setOverrideCount(1);
            adapter.notifyDataSetChanged();
            subNavSelector.setSelectedIndex(0);
            pager.setCurrentItem(0);
            adapter.notifyDataSetChanged();
        }
    }

    private void transitionInSubNavBar() {
        if (subNavSelector == null) {
            return;
        }
        subNavSelector.setVisibility(View.INVISIBLE);
        Views.runWhenLaidOut(subNavSelector, stateSafeExecutor.bind(() -> {
            if (subNavSelector != null) {
                subNavSelector.setTranslationY(-subNavSelector.getMeasuredHeight());
                subNavSelector.setVisibility(View.VISIBLE);
                animatorFor(subNavSelector, getAnimatorContext())
                        .translationY(0f)
                        .start();
            }
        }));
    }

    private void transitionOutSubNavBar() {
        if (subNavSelector == null) {
            return;
        }
        animatorFor(subNavSelector, getAnimatorContext())
                .translationY(-subNavSelector.getMeasuredHeight())
                .addOnAnimationCompleted(finished -> {
                    if (finished && subNavSelector != null) {
                        subNavSelector.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    @Override
    public void onRefresh() {
        sleepSoundsPresenter.update();
        swipeRefreshLayout.setRefreshing(true);
        for (int i = 0; i < adapter.getCount(); i++) {
            SubFragment fragment = getSubFragment(i);
            if (fragment != null) {
                fragment.update();
            }
        }
    }

}
