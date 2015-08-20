package is.hello.sense.ui.widget.timeline;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.TimelineEvent;
import rx.functions.Action1;

public class TimelineInfoOverlay implements Handler.Callback {
    private static final long DISPLAY_DURATION = 3000;
    private static final int MSG_DISMISS = 0x1;

    private final Activity activity;
    private final AnimatorContext animatorContext;

    private final Handler delayHandler = new Handler(Looper.getMainLooper(), this);
    private final Resources resources;
    private final PopupWindow popupWindow;

    private final FrameLayout contents;
    private final TextView tooltip;

    private @Nullable Action1<TimelineInfoOverlay> onDismiss;

    public TimelineInfoOverlay(@NonNull Activity activity,
                               @NonNull AnimatorContext animatorContext) {
        this.activity = activity;
        this.animatorContext = animatorContext;

        this.resources = activity.getResources();
        this.popupWindow = new PopupWindow(activity);
        popupWindow.setBackgroundDrawable(null);
        popupWindow.setTouchable(false);
        popupWindow.setWindowLayoutMode(LayoutParams.MATCH_PARENT,
                                        LayoutParams.MATCH_PARENT);

        this.contents = new FrameLayout(activity);
        popupWindow.setContentView(contents);

        this.tooltip = new TextView(activity);
        tooltip.setTextAppearance(activity, R.style.AppTheme_Text_Timeline);
        tooltip.setTextColor(resources.getColor(R.color.white));
        tooltip.setBackgroundResource(R.drawable.background_timeline_info_popup);

        int paddingHorizontal = resources.getDimensionPixelSize(R.dimen.gap_medium),
            paddingVertical = resources.getDimensionPixelSize(R.dimen.gap_small);
        tooltip.setPadding(tooltip.getPaddingLeft() + paddingHorizontal,
                           tooltip.getPaddingTop() + paddingVertical,
                           tooltip.getPaddingRight() + paddingHorizontal,
                           tooltip.getPaddingBottom() + paddingVertical);

        @SuppressLint("RtlHardcoded")
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                     LayoutParams.WRAP_CONTENT,
                                                     Gravity.BOTTOM | Gravity.LEFT);
        layoutParams.leftMargin = resources.getDimensionPixelSize(R.dimen.timeline_event_popup_left_inset);
        contents.addView(tooltip, layoutParams);
    }

    public void bindEvent(@NonNull TimelineEvent event) {
        CharSequence prefix = activity.getText(R.string.timeline_popup_info_prefix);
        CharSequence sleepDepth = activity.getText(event.getSleepState().stringRes);

        SpannableStringBuilder reading = new SpannableStringBuilder(prefix).append(sleepDepth);
        tooltip.setText(reading);
    }

    public void setOnDismiss(@Nullable Action1<TimelineInfoOverlay> onDismiss) {
        this.onDismiss = onDismiss;
    }

    private Drawable createBackground(@NonNull Point screenSize,
                                      int viewTop,
                                      int viewBottom) {
        final Path fillPath = new Path();
        fillPath.addRect(0, 0,
                         screenSize.x, viewTop,
                         Path.Direction.CW);

        int gutterSize = resources.getDimensionPixelSize(R.dimen.timeline_segment_item_end_inset);
        fillPath.addRect(screenSize.x - gutterSize, viewTop,
                         screenSize.x, viewBottom,
                         Path.Direction.CW);

        fillPath.addRect(0, viewBottom,
                         screenSize.x, screenSize.y,
                         Path.Direction.CW);

        ShapeDrawable background = new ShapeDrawable(new PathShape(fillPath,
                                                                   screenSize.x,
                                                                   screenSize.y));
        background.getPaint().setColor(resources.getColor(R.color.background_light_overlay));
        return background;
    }

    public void show(@NonNull View fromView, boolean animate) {
        if (popupWindow.isShowing()) {
            return;
        }

        final Rect viewFrame = new Rect();
        fromView.getGlobalVisibleRect(viewFrame);

        final Point screenSize = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final View contentRoot = activity.findViewById(Window.ID_ANDROID_CONTENT);
            screenSize.x = contentRoot.getWidth();
            screenSize.y = contentRoot.getHeight();

            viewFrame.top -= contentRoot.getTop();
            viewFrame.bottom -= contentRoot.getTop();
        } else {
            activity.getWindowManager()
                    .getDefaultDisplay()
                    .getSize(screenSize);
        }

        contents.setBackground(createBackground(screenSize, viewFrame.top, viewFrame.bottom));

        int tooltipBottomMargin = resources.getDimensionPixelSize(R.dimen.timeline_event_popup_bottom_inset);
        LayoutParams layoutParams = (FrameLayout.LayoutParams) tooltip.getLayoutParams();
        layoutParams.bottomMargin = (screenSize.y - viewFrame.top) + tooltipBottomMargin;
        tooltip.requestLayout();

        popupWindow.showAtLocation(fromView, Gravity.TOP, 0, 0);

        if (animate) {
            contents.setAlpha(0f);
            // Allow for layout pass
            contents.post(() -> {
                tooltip.setTranslationY(tooltipBottomMargin);

                animatorContext.transaction(t -> {
                    t.animatorFor(contents)
                     .alpha(1f);

                    t.animatorFor(tooltip)
                     .translationY(0f);
                }, finished -> {
                    if (!finished) {
                        return;
                    }

                    delayHandler.removeMessages(MSG_DISMISS);
                    delayHandler.sendEmptyMessageDelayed(MSG_DISMISS, DISPLAY_DURATION);
                });
            });
        } else {
            delayHandler.removeMessages(MSG_DISMISS);
            delayHandler.sendEmptyMessageDelayed(MSG_DISMISS, DISPLAY_DURATION);
        }
    }

    public void dismiss(boolean animate) {
        delayHandler.removeMessages(MSG_DISMISS);

        if (animate) {
            animatorContext.transaction(t -> {
                int tooltipBottomMargin = resources.getDimensionPixelSize(R.dimen.gap_xsmall);
                t.animatorFor(tooltip)
                 .translationY(tooltipBottomMargin);

                t.animatorFor(contents)
                 .alpha(0f);
            }, finished -> {
                popupWindow.dismiss();

                if (onDismiss != null) {
                    onDismiss.call(this);
                }
            });
        } else {
            Anime.cancelAll(contents, tooltip);
            popupWindow.dismiss();
            contents.setAlpha(0f);

            if (onDismiss != null) {
                onDismiss.call(this);
            }
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_DISMISS) {
            dismiss(true);
            return true;
        }

        return false;
    }
}
