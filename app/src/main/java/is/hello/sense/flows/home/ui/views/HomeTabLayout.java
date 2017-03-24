package is.hello.sense.flows.home.ui.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.home.util.HomeFragmentPagerAdapter;

/**
 * To be used in the HomeView.
 * Handles updating sleep score tab and tab unread indicator
 */

public class HomeTabLayout extends SenseTabLayout<HomeTabLayout.Listener> {

    public HomeTabLayout(final Context context) {
        this(context, null, 0);
    }

    public HomeTabLayout(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeTabLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View createTabCustomView(final int position,
                                       @NonNull final HomeFragmentPagerAdapter adapter,
                                       @NonNull final HomeFragmentPagerAdapter.HomeItem item) {
        if(position == adapter.getTimelineItemPosition()) {
            return createSleepScoreTabView(getCurrentTimeline());
        } else {
            return super.createTabCustomView(position, adapter, item);
        }
    }

    //region tab indicator

    /**
     * Note: does nothing if current tab position equals position
     * or no tab was found at position
     */
    public void setTabIndicatorVisible(final boolean show,
                                       final int position) {
        if (getSelectedTabPosition() == position) {
            return;
        }
        final TabLayout.Tab tab = getTabAt(position);
        if (tab == null) {
            return;
        }
        if (tab.getCustomView() instanceof SenseTabView) {
            ((SenseTabView) tab.getCustomView()).setIndicatorVisible(show);
        }
    }

    //endregion

    //region sleep score

    public void updateTabWithSleepScore(@Nullable final Timeline timeline,
                                        final int position) {
        final TabLayout.Tab tab = getTabAt(position);
        if (tab == null) {
            return;
        }
        final View view = tab.getCustomView();
        if (view instanceof SenseTabView) {
            ((SenseTabView) view).useSleepScoreIcon(timeline != null ? timeline.getScore() : null)
                                 .setActive(tab.isSelected());
        }

    }

    @Nullable
    private Timeline getCurrentTimeline() {
        if (this.listener != null) {
            return this.listener.getCurrentTimeline();
        }
        return null;

    }

    private SenseTabView createSleepScoreTabView(@Nullable final Timeline timeline) {
        final Integer score = timeline != null ? timeline.getScore() : null;
        return new SenseTabView(getContext())
                .useSleepScoreIcon(score)
                .setActive(false);
    }

    public interface Listener extends SenseTabLayout.Listener {

        @Nullable
        Timeline getCurrentTimeline();
    }

    //endregion
}
