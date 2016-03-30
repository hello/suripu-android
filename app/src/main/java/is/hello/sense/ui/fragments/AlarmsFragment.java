package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.fragments.settings.AppSettingsFragment;
import is.hello.sense.ui.widget.ExtendedScrollView;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.SelectorView.OnSelectionChangedListener;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class AlarmsFragment extends BacksideTabFragment implements OnSelectionChangedListener {

    private ProgressBar initialActivityIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SelectorView timeScaleSelector;
    private ExtendedViewPager pager;
    private StaticFragmentAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_alarms, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_alarms_refresh_container);
        // todo change this  swipeRefreshLayout.setOnRefreshListener(this::fetchTrends);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        this.initialActivityIndicator = (ProgressBar) view.findViewById(R.id.fragment_alarms_loading);
        this.pager = (ExtendedViewPager) view.findViewById(R.id.fragment_alarms_scrollview);
        this.timeScaleSelector = (SelectorView) view.findViewById(R.id.fragment_alarms_time_scale);
        timeScaleSelector.setButtonLayoutParams(new SelectorView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        timeScaleSelector.setVisibility(View.INVISIBLE);
        timeScaleSelector.setBackground(new TabsBackgroundDrawable(getResources(),
                                                                   TabsBackgroundDrawable.Style.TRENDS));
        timeScaleSelector.addOption(R.string.alarm_subnavbar_alarm_list, false);
        timeScaleSelector.addOption(R.string.alarm_subnavbar_sounds_list, false);
        timeScaleSelector.setOnSelectionChangedListener(this);

        timeScaleSelector.setVisibility(View.INVISIBLE);
        Views.runWhenLaidOut(timeScaleSelector, stateSafeExecutor.bind(() -> {
            timeScaleSelector.setTranslationY(-timeScaleSelector.getMeasuredHeight());
            timeScaleSelector.setVisibility(View.VISIBLE);
            animatorFor(timeScaleSelector, getAnimatorContext())
                    .translationY(0f)
                    .start();
        }));

        this.adapter = new StaticFragmentAdapter(getChildFragmentManager(),
                                                 new StaticFragmentAdapter.Item(SmartAlarmListFragment.class, getString(R.string.alarm_subnavbar_alarm_list)),
                                                 new StaticFragmentAdapter.Item(SenseSoundsFragment.class, getString(R.string.alarm_subnavbar_sounds_list)));
        pager.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout.setRefreshing(true);
        timeScaleSelector.setSelectedIndex(0);
        onUpdate();// todo erase
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
    public void onSelectionChanged(int newSelectionIndex) {
        pager.setCurrentItem(newSelectionIndex);
    }
}
