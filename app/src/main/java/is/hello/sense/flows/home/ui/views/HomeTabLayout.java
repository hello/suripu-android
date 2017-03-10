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

public class HomeTabLayout extends SenseTabLayout {

    @Nullable
    private Listener listener = null;

    public HomeTabLayout(Context context) {
        super(context);
    }

    public HomeTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HomeTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setListener(@Nullable final SenseTabLayout.Listener listener) {
        super.setListener(listener);
        if (listener instanceof Listener) {
            this.listener = (HomeTabLayout.Listener) listener;
        } else {
            this.listener = null;
        }
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

    public void setTabIndicatorVisible(final boolean show, final int position) {
        if (getSelectedTabPosition() == position) {
            return;
        }
        setTabIndicatorVisible(position, show);
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
