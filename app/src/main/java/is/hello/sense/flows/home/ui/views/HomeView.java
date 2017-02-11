package is.hello.sense.flows.home.ui.views;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.databinding.ViewHomeBinding;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.flows.home.util.HomeViewPagerPresenterDelegate;
import is.hello.sense.mvp.view.BindedPresenterView;

@SuppressLint("ViewConstructor")
public class HomeView extends BindedPresenterView<ViewHomeBinding> {
    public HomeView(@NonNull final Activity activity,
                    @NonNull final HomeViewPagerPresenterDelegate delegate,
                    @NonNull final FragmentManager fragmentManager) {
        super(activity);
        this.binding.viewHomeExtendedViewPager.setScrollingEnabled(false);
        this.binding.viewHomeExtendedViewPager.setFadePageTransformer(true);
        this.binding.viewHomeExtendedViewPager.setOffscreenPageLimit(delegate.getOffscreenPageLimit());
        this.binding.viewHomeTabLayout.setupWithViewPager(this.binding.viewHomeExtendedViewPager);
        final StaticFragmentAdapter fragmentAdapter = new StaticFragmentAdapter(fragmentManager,
                                                                                this.binding.viewHomeExtendedViewPager.getId(),
                                                                                delegate.getViewPagerItems());
        this.binding.viewHomeExtendedViewPager.setAdapter(fragmentAdapter);

    }

    //region BindedPresenterView

    @Override
    protected int getLayoutRes() {
        return R.layout.view_home;
    }

    @Override
    public void releaseViews() {
        this.binding.viewHomeTabLayout.setListener(null);
        this.binding.viewHomeTabLayout.clearOnTabSelectedListeners();
        this.binding.viewHomeExtendedViewPager.setAdapter(null);

    }

    //endregion

    //region methods

    /**
     * Forwards the index position of the starting fragment. Will not have an immediate effect until
     * {@link #setUpTabs(boolean)} is called.
     *
     * @param itemIndex index position of the starting fragment to show.
     */
    public void setTabLayoutCurrentItemIndex(final int itemIndex) {
        this.binding.viewHomeTabLayout.setCurrentItemIndex(itemIndex);
    }

    /**
     * Will create the tabs.
     *
     * @param shouldSelect if true will select the tab at the position provided with
     *                     {@link #setTabLayoutCurrentItemIndex(int)}. If none were given, 0 will be
     *                     default.
     */
    public void setUpTabs(final boolean shouldSelect) {
        this.binding.viewHomeTabLayout.setUpTabs(shouldSelect);
    }

    /**
     * @param listener listener for SenseTabLayout events.
     */
    public void setTabListener(@NonNull final SenseTabLayout.Listener listener) {
        this.binding.viewHomeTabLayout.setListener(listener);
    }

    /**
     * @return index position of the currently selected tab.
     */
    public int getTabLayoutSelectedPosition() {
        return this.binding.viewHomeTabLayout.getSelectedTabPosition();
    }

    /**
     * Sets the ExtendedViewPagers current item.
     *
     * @param item index of fragment to show.
     */
    public void setExtendedViewPagerCurrentItem(final int item) {
        this.binding.viewHomeExtendedViewPager.setCurrentItem(item);
    }

    /**
     * @param index position of fragment we need.
     * @return fragment if found else null.
     */
    @Nullable
    public Fragment getFragmentWithIndex(final int index) {
        final PagerAdapter adapter = this.binding.viewHomeExtendedViewPager.getAdapter();
        if (adapter instanceof StaticFragmentAdapter) {
            return ((StaticFragmentAdapter) adapter).getFragment(index);
        }
        return null;
    }

    /**
     * Displays/Hides the blue indicator on the Feed Tab
     *
     * @param show true to show.
     */
    public void showUnreadIndicatorOnFeedTab(final boolean show) {
        this.binding.viewHomeTabLayout.setFeedTabIndicatorVisible(show);
    }

    /**
     * Update the current sleep score icon for the given timeline.
     *
     * @param timeline if null will default to no data state.
     */
    public void updateSleepScoreIcon(@Nullable final Timeline timeline) {
        this.binding.viewHomeTabLayout.updateSleepScoreTab(timeline);
    }

    /**
     * @return true if the progress bar is currenty showing.
     */
    public boolean isProgressOverlayShowing() {
        return this.binding.viewHomeProgressOverlay.getVisibility() == VISIBLE;
    }

    /**
     * @param show true to display and false to hide.
     */
    public void showProgressOverlay(final boolean show) {
        this.binding.viewHomeProgressOverlay.post(() -> {
            if (show) {
                this.binding.viewHomeProgressOverlay.bringToFront();
                this.binding.viewHomeSpinner.startSpinning();
                this.binding.viewHomeProgressOverlay.setVisibility(View.VISIBLE);
            } else {
                this.binding.viewHomeSpinner.stopSpinning();
                this.binding.viewHomeProgressOverlay.setVisibility(View.GONE);
            }
        });
    }
    //endregion
}