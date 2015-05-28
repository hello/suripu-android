package is.hello.sense.ui.widget.timeline;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import is.hello.sense.R;

public class TimelineInfoPopup {
    private final Activity activity;
    private final PopupWindow popupWindow;
    private final TextView contents;

    public TimelineInfoPopup(@NonNull Activity activity) {
        this.activity = activity;

        this.popupWindow = new PopupWindow(activity);
        popupWindow.setAnimationStyle(R.style.WindowAnimations_PopSlideAndFade);
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable()); // Required for touch to dismiss
        popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        this.contents = new TextView(activity);
        contents.setTextAppearance(activity, R.style.AppTheme_Text_Body);
        contents.setBackgroundResource(R.drawable.background_timeline_event);

        Resources resources = activity.getResources();
        int paddingHorizontal = resources.getDimensionPixelSize(R.dimen.gap_outer),
            paddingVertical = resources.getDimensionPixelSize(R.dimen.gap_medium);
        contents.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        popupWindow.setContentView(contents);
    }

    private int getNavigationBarHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Display display = activity.getWindowManager().getDefaultDisplay();

            Point realSize = new Point();
            display.getRealSize(realSize);

            Point visibleArea = new Point();
            display.getSize(visibleArea);

            // Status bar is counted as part of the display height,
            // so the delta just gives us the navigation bar height.
            return realSize.y - visibleArea.y;
        } else {
            return 0;
        }
    }

    public void setText(@Nullable CharSequence text) {
        contents.setText(text);
    }

    public void setText(@StringRes int textRes) {
        contents.setText(textRes);
    }

    public void show(@NonNull View fromView, long howLong) {
        View parent = (View) fromView.getParent();
        int parentHeight = parent.getMeasuredHeight();
        int bottomInset = parentHeight - fromView.getTop() + getNavigationBarHeight();
        int startInset = activity.getResources().getDimensionPixelSize(R.dimen.gap_tiny);
        popupWindow.showAtLocation(parent, Gravity.BOTTOM | Gravity.START, startInset, bottomInset);

        contents.postDelayed(this::dismiss, howLong);
    }

    public void dismiss() {
        popupWindow.dismiss();
    }
}
