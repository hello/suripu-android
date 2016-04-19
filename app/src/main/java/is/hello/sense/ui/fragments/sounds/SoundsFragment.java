package is.hello.sense.ui.fragments.sounds;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.fragments.BacksideTabFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.SelectorView.OnSelectionChangedListener;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.util.Constants;


public class SoundsFragment extends BacksideTabFragment implements OnSelectionChangedListener {

    private ProgressBar initialActivityIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SelectorView subNavSelector;
    private ExtendedViewPager pager;
    private StaticFragmentAdapter adapter;
    private RelativeLayout tempHolder;

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

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sounds, container, false);
        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_sounds_refresh_container);
        swipeRefreshLayout.setEnabled(false);
        this.initialActivityIndicator = (ProgressBar) view.findViewById(R.id.fragment_sounds_loading);
        this.pager = (ExtendedViewPager) view.findViewById(R.id.fragment_sounds_scrollview);
        this.subNavSelector = (SelectorView) view.findViewById(R.id.fragment_sounds_sub_nav);
        this.tempHolder = (RelativeLayout) view.findViewById(R.id.fragment_sounds_single_fragment_view);
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
        final boolean hasSleepSounds = getActivity().getSharedPreferences(Constants.INTERNAL_PREFS, 0).getBoolean(Constants.INTERNAL_PREF_BACKSIDE_HAS_SLEEP_SOUNDS, false);
        displayWithSleepSounds(hasSleepSounds);
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
        subNavSelector.setSelectedIndex(pager.getCurrentItem());
        swipeRefreshLayout.setRefreshing(true);
        onUpdate();
    }

    @Override
    public void onResume() {
        super.onResume();
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

    public void displayWithSleepSounds(final boolean hasSleepSounds) {
        if (tempHolder == null || subNavSelector == null || swipeRefreshLayout == null) {
            return;
        }
        tempHolder.removeAllViews();
        if (hasSleepSounds) {
            tempHolder.setVisibility(View.GONE);
            subNavSelector.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setVisibility(View.VISIBLE);
        } else {
            tempHolder.setVisibility(View.VISIBLE);
            subNavSelector.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.GONE);
            final Fragment fragment = new SmartAlarmListFragment();
            final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.add(tempHolder.getId(), fragment).commit();

        }
    }
}
