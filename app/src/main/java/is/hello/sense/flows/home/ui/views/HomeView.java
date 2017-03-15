package is.hello.sense.flows.home.ui.views;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.databinding.ViewHomeBinding;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.flows.home.util.HomeFragmentPagerAdapter;
import is.hello.sense.mvp.view.BindedPresenterView;

@SuppressLint("ViewConstructor")
public class HomeView extends BindedPresenterView<ViewHomeBinding> {
    public HomeView(@NonNull final Activity activity,
                    final int offScreenLimit,
                    @NonNull final HomeFragmentPagerAdapter fragmentAdapter) {
        super(activity);
        this.binding.viewHomeExtendedViewPager.setScrollingEnabled(false);
        this.binding.viewHomeExtendedViewPager.setFadePageTransformer(true);
        this.binding.viewHomeExtendedViewPager.setOffscreenPageLimit(offScreenLimit);
        this.binding.viewHomeExtendedViewPager.setAdapter(fragmentAdapter);
        this.binding.viewHomeTabLayout.setupWithViewPager(this.binding.viewHomeExtendedViewPager);
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
     * @param listener listener for SenseTabLayout events.
     */
    public void setTabListener(@NonNull final SenseTabLayout.Listener listener) {
        this.binding.viewHomeTabLayout.setListener(listener);
    }

    /**
     * Sets the ExtendedViewPagers current item.
     *
     * @param item index of fragment to show.
     */
    public void setCurrentItem(final int item) {
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
            return ((StaticFragmentAdapter) adapter).findFragment(index);
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
    public void updateSleepScoreTab(@Nullable final Timeline timeline) {
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

    public void selectTimelineTab() {
        this.binding.viewHomeTabLayout.selectTimelineTab();
    }

    public void selectTrendsTab() {
        this.binding.viewHomeTabLayout.selectTrendsTab();
    }

    public void selectFeedTab() {
        this.binding.viewHomeTabLayout.selectFeedTab();
    }

    public void selectSoundTab() {
        this.binding.viewHomeTabLayout.selectSoundTab();
    }

    public void selectConditionsTab() {
        this.binding.viewHomeTabLayout.selectConditionsTab();
    }
    //endregion
}