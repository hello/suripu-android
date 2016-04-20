package is.hello.sense.ui.fragments.sounds;

import android.app.Fragment;
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
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.SleepSoundsPresenter;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.fragments.BacksideTabFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.SelectorView.OnSelectionChangedListener;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.animators.MultiAnimator.animatorFor;


public class SoundsFragment extends BacksideTabFragment implements OnSelectionChangedListener {

    private ProgressBar initialActivityIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SelectorView subNavSelector;
    private ExtendedViewPager pager;
    private StaticFragmentAdapter adapter;
    private boolean hasSounds = true;
    private boolean hasSense = true;
    private boolean isShowingSounds = false;

    @Inject
    SleepSoundsPresenter sleepSoundsPresenter;

    @Inject
    DevicesPresenter devicesPresenter;

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (pager != null && adapter != null && pager.getChildCount() == adapter.getCount()) {
            final long itemId = adapter.getItemId(pager.getCurrentItem());
            final String tag = "android:switcher:" + pager.getId() + ":" + itemId;
            final Fragment fragment = getChildFragmentManager().findFragmentByTag(tag);
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
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sounds, container, false);
        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_sounds_refresh_container);
        swipeRefreshLayout.setEnabled(false);
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
        subNavSelector.setVisibility(View.VISIBLE);

        this.adapter = new StaticFragmentAdapter(getChildFragmentManager(),
                                                 new StaticFragmentAdapter.Item(SmartAlarmListFragment.class, getString(R.string.alarm_subnavbar_alarm_list)),
                                                 new StaticFragmentAdapter.Item(SleepSoundsFragment.class, getString(R.string.alarm_subnavbar_sounds_list)));

        pager.setAdapter(adapter);
        pager.setFadePageTransformer(true);
        displayWithSleepSounds(hasSense && hasSounds);
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
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sleepSoundsPresenter.update();
        devicesPresenter.update();
        bindAndSubscribe(sleepSoundsPresenter.sounds, this::bindSleepSounds, this::presentError);
        bindAndSubscribe(devicesPresenter.devices, this::bindDevices, this::presentError);
        subNavSelector.setSelectedIndex(pager.getCurrentItem());
        swipeRefreshLayout.setRefreshing(true);
        onUpdate();
    }

    @Override
    public void onResume() {
        super.onResume();
        devicesPresenter.update();
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

    public void bindSleepSounds(@NonNull SleepSounds sleepSounds) {

        hasSounds = sleepSounds.getSounds() != null && !sleepSounds.getSounds().isEmpty();
        displayWithSleepSounds(hasSounds && hasSense);
    }

    public void bindDevices(@NonNull Devices devices) {
        hasSense = !(devices.getSense() == null || devices.getSense().isMissing());
        displayWithSleepSounds(hasSounds && hasSense);
    }


    public void presentError(@NonNull Throwable error) {
        //todo check again?
    }


    public void displayWithSleepSounds(final boolean hasSleepSounds) {
        if (subNavSelector == null || swipeRefreshLayout == null) {
            return;
        }
        if (hasSleepSounds && !isShowingSounds) {
            isShowingSounds = true;
            transitionInSubNavBar();
            adapter.setOverrideCount(-1);
            adapter.notifyDataSetChanged();
        } else if (!hasSleepSounds) {
            isShowingSounds = false;
            transitionOutSubNavBar();
            adapter.setOverrideCount(1);
            adapter.notifyDataSetChanged();
            subNavSelector.setSelectedIndex(0);
            pager.setCurrentItem(0);
            adapter.notifyDataSetChanged();
        }
    }

    private void transitionInSubNavBar() {
        subNavSelector.setVisibility(View.INVISIBLE);
        Views.runWhenLaidOut(subNavSelector, stateSafeExecutor.bind(() -> {
            subNavSelector.setTranslationY(-subNavSelector.getMeasuredHeight());
            subNavSelector.setVisibility(View.VISIBLE);
            animatorFor(subNavSelector, getAnimatorContext())
                    .translationY(0f)
                    .start();
        }));
    }

    private void transitionOutSubNavBar() {
        animatorFor(subNavSelector, getAnimatorContext())
                .translationY(-subNavSelector.getMeasuredHeight())
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        subNavSelector.setVisibility(View.GONE);
                    }
                })
                .start();
    }

}
