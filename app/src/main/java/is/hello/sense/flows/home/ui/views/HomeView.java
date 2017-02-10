package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.flows.home.ui.fragments.HomeFragment;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SpinnerImageView;

@SuppressLint("ViewConstructor")
public class HomeView extends PresenterView {

    private final View progressOverlay;
    private final SpinnerImageView spinner;
    private final ExtendedViewPager extendedViewPager;
    private final SenseTabLayout tabLayout;

    public HomeView(@NonNull final Activity activity,
                    @NonNull final FragmentManager childFragmentManager,
                    @NonNull final HomeFragment.HomeViewPagerDelegate viewPagerDelegate,
                    @NonNull final SenseTabLayout.Listener listener) {
        super(activity);
        this.progressOverlay = findViewById(R.id.view_home_progress_overlay);
        this.spinner = (SpinnerImageView) this.progressOverlay.findViewById(R.id.view_home_spinner);
        this.extendedViewPager = (ExtendedViewPager) findViewById(R.id.view_home_extended_view_pager);
        this.tabLayout = (SenseTabLayout) findViewById(R.id.view_home_tab_layout);
        this.extendedViewPager.setScrollingEnabled(false);
        this.extendedViewPager.setFadePageTransformer(true);
        this.extendedViewPager.setOffscreenPageLimit(viewPagerDelegate.getOffscreenPageLimit());
        this.tabLayout.setupWithViewPager(this.extendedViewPager);
        final StaticFragmentAdapter fragmentAdapter = new StaticFragmentAdapter(childFragmentManager,
                                                                                this.extendedViewPager.getId(),
                                                                                viewPagerDelegate.getViewPagerItems());
        this.extendedViewPager.setAdapter(fragmentAdapter);
        this.tabLayout.setListener(listener);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_home;
    }

    @Override
    public void releaseViews() {
        this.tabLayout.setListener(null);
        this.tabLayout.clearOnTabSelectedListeners();
        this.extendedViewPager.setAdapter(null);
    }

    public void setUpTabs(final boolean shouldSelect) {
        this.tabLayout.setUpTabs(shouldSelect);
    }

    public void setCurrentItemIndex(final int index) {
        this.tabLayout.setCurrentItemIndex(index);
    }

    public int getSelectedTabPosition() {
        return this.tabLayout.getSelectedTabPosition();
    }

    public void updateSleepScoreTab(@NonNull final Timeline timeline) {
        this.tabLayout.updateSleepScoreTab(timeline);
    }

    public boolean isLoading() {
        return this.progressOverlay.getVisibility() == VISIBLE;
    }

    public void setUnreadItems(final boolean hasUnreadItems) {
        this.tabLayout.setHomeTabIndicatorVisible(hasUnreadItems);
    }

    public void setCurrentItem(final int fragmentPosition) {
        this.extendedViewPager.setCurrentItem(fragmentPosition);
    }

    @Nullable
    public Fragment getFragmentWithIndex(final int index) {
        if (extendedViewPager != null && extendedViewPager.getAdapter() instanceof StaticFragmentAdapter) {
            return ((StaticFragmentAdapter) extendedViewPager.getAdapter()).getFragment(index);
        }
        return null;
    }

}
