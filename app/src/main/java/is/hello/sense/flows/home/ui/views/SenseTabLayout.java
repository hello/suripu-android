package is.hello.sense.flows.home.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable;

public class SenseTabLayout extends TabLayout
        implements TabLayout.OnTabSelectedListener {

    private static final int NUMBER_OF_ITEMS = 5;
    public static final int SLEEP_ICON_KEY = 0;
    public static final int TRENDS_ICON_KEY = 1;
    public static final int INSIGHTS_ICON_KEY = 2;
    public static final int SOUNDS_ICON_KEY = 3;
    public static final int CONDITIONS_ICON_KEY = 4;

    private final Drawable[] drawables = new Drawable[NUMBER_OF_ITEMS];
    private final Drawable[] drawablesActive = new Drawable[NUMBER_OF_ITEMS];

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
        currentItemIndex = tab.getPosition();
        tabChanged(currentItemIndex);
        tab.setIcon(drawablesActive[currentItemIndex]);
        if (currentItemIndex == SLEEP_ICON_KEY) {
            jumpToLastNight();
        }

    }

    @Override
    public void onTabUnselected(final Tab tab) {
        if (tab == null) {
            return;
        }
        tab.setIcon(drawables[tab.getPosition()]);
    }

    @Override
    public void onTabReselected(final Tab tab) {
        scrollUp(tab.getPosition());

    }
    //endregion

    private void scrollUp(final int position) {
        if (listener != null) {
            listener.scrollUp(position);
        }
    }

    private void tabChanged(final int fragmentPosition) {
        if (listener != null) {
            listener.tabChanged(fragmentPosition);
        }
    }

    private void jumpToLastNight() {
        if (listener != null) {
            listener.jumpToLastNight();
        }
    }

    @Nullable
    private Timeline getCurrentTimeline() {
        if (listener != null) {
            return listener.getCurrentTimeline();
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

    public void selectInsightsTab() {
        selectTab(INSIGHTS_ICON_KEY);
    }

    public void selectConditionsTab() {
        selectTab(INSIGHTS_ICON_KEY);
    }

    private void selectTab(final int position) {
        final TabLayout.Tab tab = getTabAt(position);
        if (tab == null) {
            return;
        }
        tab.select();
    }

    public void setUpTabs(final boolean shouldSelect) {
        drawables[TRENDS_ICON_KEY] = ContextCompat.getDrawable(getContext(), R.drawable.icon_trends_24);
        drawablesActive[TRENDS_ICON_KEY] = ContextCompat.getDrawable(getContext(), R.drawable.icon_trends_active_24);
        drawables[INSIGHTS_ICON_KEY] = ContextCompat.getDrawable(getContext(), R.drawable.icon_insight_24);
        drawablesActive[INSIGHTS_ICON_KEY] = ContextCompat.getDrawable(getContext(), R.drawable.icon_insight_active_24);
        drawables[SOUNDS_ICON_KEY] = ContextCompat.getDrawable(getContext(), R.drawable.icon_sound_24);
        drawablesActive[SOUNDS_ICON_KEY] = ContextCompat.getDrawable(getContext(), R.drawable.icon_sound_active_24);
        drawables[CONDITIONS_ICON_KEY] = ContextCompat.getDrawable(getContext(), R.drawable.icon_sense_24);
        drawablesActive[CONDITIONS_ICON_KEY] = ContextCompat.getDrawable(getContext(), R.drawable.icon_sense_active_24);

        final SleepScoreIconDrawable.Builder drawableBuilder = new SleepScoreIconDrawable.Builder(getContext());
        drawableBuilder.withSize(drawables[TRENDS_ICON_KEY].getIntrinsicWidth(), drawables[TRENDS_ICON_KEY].getIntrinsicHeight());
        final Timeline currentTimeline = getCurrentTimeline();
        if (currentTimeline == null) {
            drawables[SLEEP_ICON_KEY] = drawableBuilder.build();
            drawablesActive[SLEEP_ICON_KEY] = drawableBuilder.withSelected(true).build();
        } else {
            updateSleepScoreTab(currentTimeline);
        }
        removeAllTabs();
        addTab(newTab().setIcon(drawables[SLEEP_ICON_KEY]));
        addTab(newTab().setIcon(drawables[TRENDS_ICON_KEY]));
        addTab(newTab().setIcon(drawables[INSIGHTS_ICON_KEY]));
        addTab(newTab().setIcon(drawables[SOUNDS_ICON_KEY]));
        addTab(newTab().setIcon(drawables[CONDITIONS_ICON_KEY]));
        clearOnTabSelectedListeners();
        addOnTabSelectedListener(this);
        final TabLayout.Tab tab = getTabAt(currentItemIndex);
        if (shouldSelect && tab != null) {
            tab.setIcon(drawablesActive[currentItemIndex]);
            tab.select();
        }
    }

    public void updateSleepScoreTab(@Nullable final Timeline timeline) {
        final SleepScoreIconDrawable.Builder drawableBuilder = new SleepScoreIconDrawable.Builder(getContext());
        drawableBuilder.withSize(drawables[TRENDS_ICON_KEY].getIntrinsicWidth(), drawables[TRENDS_ICON_KEY].getIntrinsicHeight());
        if (timeline != null &&
                timeline.getScore() != null) {
            if (TimelineInteractor.hasValidCondition(timeline)) {
                drawableBuilder.withText(timeline.getScore());
            }
        }
        drawables[SLEEP_ICON_KEY] = drawableBuilder.build();
        drawablesActive[SLEEP_ICON_KEY] = drawableBuilder.withSelected(true).build();
        final TabLayout.Tab tab = getTabAt(SLEEP_ICON_KEY);
        if (tab == null) {
            return;
        }
        drawableBuilder.withSelected(tab.isSelected());
        tab.setIcon(drawableBuilder.build());
    }

    public void setCurrentItemIndex(final int index) {
        this.currentItemIndex = index;
    }

    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void scrollUp(int fragmentPosition);

        void jumpToLastNight();

        void tabChanged(int fragmentPosition);

        @Nullable
        Timeline getCurrentTimeline();
    }
}
