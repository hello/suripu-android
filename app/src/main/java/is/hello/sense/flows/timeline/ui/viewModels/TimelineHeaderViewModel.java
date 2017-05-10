package is.hello.sense.flows.timeline.ui.viewModels;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import is.hello.sense.R;

public class TimelineHeaderViewModel {
    private static final int HIDE_ICON = 0;

    private final String title;
    @DrawableRes
    private final int historyIcon;
    @DrawableRes
    private final int shareIcon;

    private final ActionHandler actionHandler;

    public static TimelineHeaderViewModel getInstance(@NonNull final String title,
                                                      @NonNull final ActionHandler actionHandler) {
        return new TimelineHeaderViewModel(title,
                                           R.drawable.icon_calendar_24,
                                           R.drawable.icon_share_24,
                                           actionHandler);
    }

    public static TimelineHeaderViewModel getShareOnlyInstance(@NonNull final String title,
                                                      @NonNull final ActionHandler actionHandler) {
        return new TimelineHeaderViewModel(title,
                                           HIDE_ICON,
                                           R.drawable.icon_share_24,
                                           actionHandler);
    }

    public TimelineHeaderViewModel(@NonNull final String title,
                                   @DrawableRes final int historyIcon,
                                   @DrawableRes final int shareIcon,
                                   @NonNull final ActionHandler actionHandler) {
        this.title = title;
        this.historyIcon = historyIcon;
        this.shareIcon = shareIcon;
        this.actionHandler = actionHandler;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public int getHistoryIcon() {
        return historyIcon;
    }

    public int getShareIcon() {
        return shareIcon;
    }

    @NonNull
    public ActionHandler getActionHandler() {
        return actionHandler;
    }

    public boolean showHistoryIcon() {
        return HIDE_ICON != historyIcon;
    }

    public boolean showShareIcon() {
        return false;
    }

    public interface ActionHandler {
        void onHistoryIconAction();

        void onShareIconAction();
    }
}
