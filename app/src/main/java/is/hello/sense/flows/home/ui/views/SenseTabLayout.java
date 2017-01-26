package is.hello.sense.flows.home.ui.views;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;

public class SenseTabLayout extends TabLayout
        implements TabLayout.OnTabSelectedListener {

    public static final int SLEEP_ICON_KEY = 0;
    public static final int TRENDS_ICON_KEY = 1;
    public static final int INSIGHTS_ICON_KEY = 2;
    public static final int SOUNDS_ICON_KEY = 3;
    public static final int CONDITIONS_ICON_KEY = 4;

    private Listener listener = null;
    private int currentItemIndex;

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

    //region TabSelectedListener
    @Override
    public void onTabSelected(final Tab tab) {
        if (tab == null) {
            return;
        }
        this.currentItemIndex = tab.getPosition();
        tabChanged(this.currentItemIndex);
        setTabActive(tab, true);
        if (this.currentItemIndex == SLEEP_ICON_KEY) {
            jumpToLastNight();
        }

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

    private void jumpToLastNight() {
        if (this.listener != null) {
            this.listener.jumpToLastNight();
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

    public void selectHomeTab() {
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


    public void setHomeabIndicatorVisible(final boolean show) {
        setTabIndicatorVisible(INSIGHTS_ICON_KEY, show);
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

    public void setUpTabs(final boolean shouldSelect) {
        removeAllTabs();
        addTab(createSleepScoreTab(getCurrentTimeline()));
        addTab(createTabFor(R.drawable.icon_trends_24, R.drawable.icon_trends_active_24));
        addTab(createTabFor(R.drawable.icon_insight_24, R.drawable.icon_insight_active_24));
        addTab(createTabFor(R.drawable.icon_sound_24, R.drawable.icon_sound_active_24));
        addTab(createTabFor(R.drawable.icon_sense_24, R.drawable.icon_sense_active_24));

        clearOnTabSelectedListeners();
        addOnTabSelectedListener(this);

        final TabLayout.Tab tab = getTabAt(this.currentItemIndex);
        if (shouldSelect && tab != null) {
            setTabActive(tab, true);
            tab.select();
        }
    }

    private Tab createSleepScoreTab(@Nullable final Timeline timeline) {
        return newTab().setCustomView(new SenseTabView(getContext())
                                              .useSleepScoreIcon(timeline));
    }

    private Tab createTabFor(@DrawableRes final int normal,
                             @DrawableRes final int active) {
        return newTab().setCustomView(new SenseTabView(getContext())
                                              .setDrawables(normal, active)
                                              .setActive(false));
    }

    public void setCurrentItemIndex(final int index) {
        this.currentItemIndex = index;
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

    public void updateSleepScoreTab(@Nullable final Timeline timeline) {
        final TabLayout.Tab tab = getTabAt(SLEEP_ICON_KEY);
        if (tab == null) {
            return;
        }
        final View view = tab.getCustomView();
        if (view instanceof SenseTabView) {
            ((SenseTabView) view).updateSleepScoreIcon(timeline, tab.isSelected());
        }

    }

    public interface Listener {
        void scrollUp(int fragmentPosition);

        void jumpToLastNight();

        void tabChanged(int fragmentPosition);

        @Nullable
        Timeline getCurrentTimeline();
    }
}
