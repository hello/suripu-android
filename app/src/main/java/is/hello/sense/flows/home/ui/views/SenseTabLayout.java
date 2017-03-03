package is.hello.sense.flows.home.ui.views;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.home.util.HomeFragmentPagerAdapter;

public class SenseTabLayout extends TabLayout
        implements TabLayout.OnTabSelectedListener {

    public static final int SLEEP_ICON_KEY = 0;
    public static final int TRENDS_ICON_KEY = 1;
    public static final int INSIGHTS_ICON_KEY = 2;
    public static final int SOUNDS_ICON_KEY = 3;
    public static final int CONDITIONS_ICON_KEY = 4;

    @Nullable
    private Listener listener = null;

    public SenseTabLayout(final Context context) {
        this(context, null, 0);
    }

    public SenseTabLayout(final Context context,
                          final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SenseTabLayout(final Context context,
                          final AttributeSet attrs,
                          final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setupWithViewPager(@Nullable final ViewPager viewPager) {
        super.setupWithViewPager(viewPager);
        clearOnTabSelectedListeners();

        if (viewPager !=null && viewPager.getAdapter() instanceof HomeFragmentPagerAdapter) {
            final HomeFragmentPagerAdapter.HomeItem[] items = ((HomeFragmentPagerAdapter) viewPager.getAdapter()).getHomeItems();
            final int tabCount = getTabCount();
            if (items.length != tabCount) {
                throw new AssertionError(String.format("Tab count mismatch expected %s actual %s", items.length, tabCount));
            }
            for (int position = 0; position < tabCount; position++) {
                final HomeFragmentPagerAdapter.HomeItem item = items[position];
                Tab tab = getTabAt(position);
                if (tab == null) {
                    tab = newTab();
                    addTab(tab, position);
                }
                if (position == SLEEP_ICON_KEY) {
                    tab.setCustomView(createSleepScoreTabView(getCurrentTimeline()));
                } else {
                    tab.setCustomView(createTabFor(item.normalIcon, item.activeIcon));
                }
            }
        }

        addOnTabSelectedListener(this);

        final TabLayout.Tab tab = getTabAt(getSelectedTabPosition());
        if (tab != null) {
            setTabActive(tab, true);
            tab.select();
        }
    }

    //region TabSelectedListener
    @Override
    public void onTabSelected(final Tab tab) {
        if (tab == null) {
            return;
        }
        tabChanged(tab.getPosition());
        setTabActive(tab, true);
    }

    @Override
    public void onTabUnselected(final Tab tab) {
        setTabActive(tab, false);
    }

    @Override
    public void onTabReselected(final Tab tab) {
        scrollUp(tab.getPosition());

    }
    //endregion

    private void scrollUp(final int position) {
        if (this.listener != null) {
            this.listener.scrollUp(position);
        }
    }

    private void tabChanged(final int fragmentPosition) {
        if (this.listener != null) {
            this.listener.tabChanged(fragmentPosition);
        }
    }


    @Nullable
    private Timeline getCurrentTimeline() {
        if (this.listener != null) {
            return this.listener.getCurrentTimeline();
        }
        return null;

    }

    public void selectTimelineTab() {
        selectTab(SLEEP_ICON_KEY);
    }

    public void selectTrendsTab() {
        selectTab(TRENDS_ICON_KEY);
    }

    public void selectSoundTab() {
        selectTab(SOUNDS_ICON_KEY);
    }

    public void selectFeedTab() {
        selectTab(INSIGHTS_ICON_KEY);
    }

    public void selectConditionsTab() {
        selectTab(CONDITIONS_ICON_KEY);
    }

    private void selectTab(final int position) {
        final TabLayout.Tab tab = getTabAt(position);
        if (tab == null) {
            return;
        }
        tab.select();
    }

    public void setFeedTabIndicatorVisible(final boolean show) {
        if (getSelectedTabPosition() == INSIGHTS_ICON_KEY) {
            return;
        }
        setTabIndicatorVisible(INSIGHTS_ICON_KEY, show);
    }

    public void updateSleepScoreTab(@Nullable final Timeline timeline) {
        final TabLayout.Tab tab = getTabAt(SLEEP_ICON_KEY);
        if (tab == null) {
            return;
        }
        final View view = tab.getCustomView();
        if (view instanceof SenseTabView) {
            ((SenseTabView) view).useSleepScoreIcon(timeline != null ? timeline.getScore() : null)
                                 .setActive(tab.isSelected());
        }

    }

    private void setTabIndicatorVisible(final int position,
                                        final boolean visible) {
        final TabLayout.Tab tab = getTabAt(position);
        if (tab == null) {
            return;
        }
        if (tab.getCustomView() instanceof SenseTabView) {
            ((SenseTabView) tab.getCustomView()).setIndicatorVisible(visible);
        }

    }

    private SenseTabView createSleepScoreTabView(@Nullable final Timeline timeline) {
        final Integer score = timeline != null ? timeline.getScore() : null;
        return new SenseTabView(getContext())
                .useSleepScoreIcon(score)
                .setActive(false);
    }

    private SenseTabView createTabFor(@DrawableRes final int normal,
                             @DrawableRes final int active) {
        return new SenseTabView(getContext())
                .setDrawables(normal, active)
                .setActive(false);
    }

    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }

    private void setTabActive(@Nullable final Tab tab,
                              final boolean active) {
        if (tab == null) {
            return;
        }
        final View view = tab.getCustomView();
        if (view instanceof SenseTabView) {
            ((SenseTabView) view).setActive(active);
        }
    }

    public interface Listener {
        void scrollUp(int fragmentPosition);

        void tabChanged(int fragmentPosition);

        @Nullable
        Timeline getCurrentTimeline();
    }
}
