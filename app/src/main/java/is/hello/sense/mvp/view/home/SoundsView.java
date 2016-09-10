package is.hello.sense.mvp.view.home;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.common.SubFragment;
import is.hello.sense.ui.fragments.sounds.SleepSoundsFragment;
import is.hello.sense.ui.fragments.sounds.SmartAlarmListFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.StateSafeExecutor;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SoundsView extends PresenterView {
    public static final String ARG_HAS_NAVBAR = SoundsView.class.getName() + ".ARG_HAS_NAVBAR";

    private ProgressBar initialActivityIndicator;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SelectorView subNavSelector;
    private ExtendedViewPager pager;
    private StaticFragmentAdapter adapter;
    private AnimatorContext animatorContext;
    private StateSafeExecutor stateSafeExecutor;

    public SoundsView(@NonNull final Activity activity,
                      @NonNull final AnimatorContext animatorContext,
                      @NonNull final StateSafeExecutor stateSafeExecutor) {
        super(activity);
        this.animatorContext = animatorContext;
        this.stateSafeExecutor = stateSafeExecutor;
    }

    @NonNull
    @Override
    public final View createView(@NonNull final LayoutInflater inflater, @NonNull final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sounds, container, false);
        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_sounds_refresh_container);
        swipeRefreshLayout.setEnabled(true);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        this.initialActivityIndicator = (ProgressBar) view.findViewById(R.id.fragment_sounds_loading);
        this.pager = (ExtendedViewPager) view.findViewById(R.id.fragment_sounds_scrollview);
        this.subNavSelector = (SelectorView) view.findViewById(R.id.fragment_sounds_sub_nav);
        pager.setScrollingEnabled(false);
        subNavSelector.setButtonLayoutParams(new SelectorView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        subNavSelector.setBackground(new TabsBackgroundDrawable(context.getResources(),
                                                                TabsBackgroundDrawable.Style.SUBNAV));
        subNavSelector.addOption(R.string.alarm_subnavbar_alarm_list, false);
        subNavSelector.addOption(R.string.alarm_subnavbar_sounds_list, false);
        subNavSelector.setTranslationY(0);
        return view;
    }

    @Override
    public final void resume() {
        super.resume();
        subNavSelector.setSelectedIndex(pager.getCurrentItem());
    }

    @Override
    public final void destroyView() {
        super.destroyView();
        initialActivityIndicator = null;
        swipeRefreshLayout = null;
        subNavSelector.setOnSelectionChangedListener(null);
        subNavSelector = null;
        pager = null;
        adapter = null;
    }

    @Override
    public final void detach() {
        super.detach();
        animatorContext = null;
        stateSafeExecutor = null;
    }

    public final void setSwipeRefreshLayoutOnRefreshListener(@NonNull final SwipeRefreshLayout.OnRefreshListener listener) {
        swipeRefreshLayout.setOnRefreshListener(listener);
    }

    public final void setSubNavSelectorOnSelectionChangedListener(@NonNull final SelectorView.OnSelectionChangedListener listener) {
        subNavSelector.setOnSelectionChangedListener(listener);
    }

    public final void setAdapter(@NonNull final FragmentManager fragmentManager,
                                 @Nullable final Bundle savedInstanceState) {
        this.adapter = new StaticFragmentAdapter(fragmentManager,
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
        subNavSelector.setSelectedIndex(pager.getCurrentItem()); //todo confirm needed
        swipeRefreshLayout.setRefreshing(true);
    }

    public final boolean isShowingViews() {
        return pager != null && adapter != null && pager.getChildCount() == adapter.getCount();
    }

    public final SubFragment getCurrentSubFragment(@NonNull final FragmentManager fragmentManager) {
        return getSubFragment(fragmentManager, pager.getCurrentItem());
    }

    public final boolean isSubNavBarVisible() {
        return subNavSelector != null && subNavSelector.getVisibility() == View.VISIBLE;
    }

    public void refreshView(final boolean show) {
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

    public final void updated() {
        initialActivityIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    public final void refreshed(@NonNull final FragmentManager fragmentManager) {
        swipeRefreshLayout.setRefreshing(true);
        for (int i = 0; i < adapter.getCount(); i++) {
            final SubFragment fragment = getSubFragment(fragmentManager, i);
            if (fragment != null) {
                fragment.update();
            }
        }
    }

    public final void setPagerItem(final int newSelectionIndex) {
        pager.setCurrentItem(newSelectionIndex);
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
                animatorFor(subNavSelector, animatorContext)
                        .translationY(0f)
                        .start();
            }
        }));
    }

    private void transitionOutSubNavBar() {
        if (subNavSelector == null) {
            return;
        }
        animatorFor(subNavSelector, animatorContext)
                .translationY(-subNavSelector.getMeasuredHeight())
                .addOnAnimationCompleted(finished -> {
                    if (finished && subNavSelector != null) {
                        subNavSelector.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private SubFragment getSubFragment(@NonNull final FragmentManager fragmentManager,
                                       final int position) {
        final long itemId = adapter.getItemId(position);
        final String tag = "android:switcher:" + pager.getId() + ":" + itemId;
        return (SubFragment) fragmentManager.findFragmentByTag(tag);
    }

}
