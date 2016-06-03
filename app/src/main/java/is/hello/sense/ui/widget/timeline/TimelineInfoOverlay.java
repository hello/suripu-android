package is.hello.sense.ui.widget.timeline;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
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
    private final Dialog dialog;

    private final FrameLayout contents;
    private final TextView tooltip;

    private int darkenOverlayColor = Color.TRANSPARENT;
    private float overlaySleepDepthPercentage;

    private @Nullable Action1<TimelineInfoOverlay> onDismiss;

    public TimelineInfoOverlay(@NonNull Activity activity,
                               @NonNull AnimatorContext animatorContext) {
        this.activity = activity;
        this.animatorContext = animatorContext;

        this.resources = activity.getResources();
        this.dialog = new Dialog(activity, R.style.AppTheme_Dialog_FullScreen);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        dialog.getWindow().setWindowAnimations(R.style.WindowAnimations);
        dialog.setCancelable(true);

        this.contents = new FrameLayout(activity);
        dialog.setContentView(contents);

        this.tooltip = new TextView(activity);
        tooltip.setTextAppearance(activity, R.style.AppTheme_Text_Timeline);
        tooltip.setTextColor(ContextCompat.getColor(activity,R.color.white));
        tooltip.setBackgroundResource(R.drawable.background_timeline_info_popup);

        final int paddingHorizontal = resources.getDimensionPixelSize(R.dimen.gap_medium),
                  paddingVertical = resources.getDimensionPixelSize(R.dimen.gap_small);
        tooltip.setPadding(tooltip.getPaddingLeft() + paddingHorizontal,
                           tooltip.getPaddingTop() + paddingVertical,
                           tooltip.getPaddingRight() + paddingHorizontal,
                           tooltip.getPaddingBottom() + paddingVertical);

        @SuppressLint("RtlHardcoded")
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                           LayoutParams.WRAP_CONTENT,
                                                           Gravity.BOTTOM | Gravity.LEFT);
        layoutParams.leftMargin = resources.getDimensionPixelSize(R.dimen.timeline_event_popup_left_inset);
        contents.addView(tooltip, layoutParams);
    }

    public void bindEvent(@NonNull TimelineEvent event) {
        final TimelineEvent.SleepState sleepState = event.getSleepState();

        final CharSequence prefix = activity.getText(R.string.timeline_popup_info_prefix);
        final CharSequence sleepDepth = activity.getText(sleepState.stringRes);
        final SpannableStringBuilder reading = new SpannableStringBuilder(prefix).append(sleepDepth);
        tooltip.setText(reading);

        if (sleepState == TimelineEvent.SleepState.LIGHT) {
            this.darkenOverlayColor = ContextCompat.getColor(activity, sleepState.colorRes);
        } else {
            this.darkenOverlayColor = Color.TRANSPARENT;
        }
        this.overlaySleepDepthPercentage = Math.min(1f, event.getSleepDepth() / 100f);
    }

    public void setOnDismiss(@Nullable Action1<TimelineInfoOverlay> onDismiss) {
        this.onDismiss = onDismiss;
    }

    private Drawable createBackground(@NonNull Point screenSize,
                                      int viewTop,
                                      int viewBottom) {
        final Path backgroundPath = new Path();
        backgroundPath.addRect(0, 0,
                               screenSize.x, viewTop,
                               Path.Direction.CW);

        final int gutterSize =
                resources.getDimensionPixelSize(R.dimen.timeline_segment_item_end_inset);
        backgroundPath.addRect(screenSize.x - gutterSize, viewTop,
                               screenSize.x, viewBottom,
                               Path.Direction.CW);

        backgroundPath.addRect(0, viewBottom,
                               screenSize.x, screenSize.y,
                               Path.Direction.CW);

        final ShapeDrawable background = new ShapeDrawable(new PathShape(backgroundPath,
                                                                         screenSize.x,
                                                                         screenSize.y));
        background.getPaint()
                  .setColor(resources.getColor(R.color.background_light_overlay));

        if (darkenOverlayColor == Color.TRANSPARENT) {
            return background;
        } else {
            final Path overlayPath = new Path();

            overlayPath.addRect(0f,
                                viewTop,
                                (screenSize.x - gutterSize) * overlaySleepDepthPercentage,
                                viewBottom,
                                Path.Direction.CW);

            final ShapeDrawable overlay = new ShapeDrawable(new PathShape(overlayPath,
                                                                          screenSize.x,
                                                                          screenSize.y));
            overlay.getPaint().setColor(darkenOverlayColor);


            final Drawable[] layers = {background, overlay};
            return new LayerDrawable(layers);
        }
    }

    public void show(@NonNull View fromView, boolean animate) {
        if (dialog.isShowing()) {
            return;
        }

        final Rect viewFrame = new Rect();
        fromView.getGlobalVisibleRect(viewFrame);

        final Point screenSize = new Point();
        final View contentRoot = activity.findViewById(Window.ID_ANDROID_CONTENT);
        screenSize.x = contentRoot.getWidth();
        screenSize.y = contentRoot.getHeight();

        viewFrame.top -= contentRoot.getTop();
        viewFrame.bottom -= contentRoot.getTop();

        contents.setBackground(createBackground(screenSize, viewFrame.top, viewFrame.bottom));

        final int tooltipBottomMargin = resources.getDimensionPixelSize(R.dimen.timeline_event_popup_bottom_inset);
        final LayoutParams layoutParams = (FrameLayout.LayoutParams) tooltip.getLayoutParams();
        layoutParams.bottomMargin = (screenSize.y - viewFrame.top) + tooltipBottomMargin;
        tooltip.requestLayout();

        dialog.show();

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
                final int tooltipBottomMargin = resources.getDimensionPixelSize(R.dimen.gap_xsmall);
                t.animatorFor(tooltip)
                 .translationY(tooltipBottomMargin);

                t.animatorFor(contents)
                 .alpha(0f);
            }, finished -> {
                dialog.dismiss();

                if (onDismiss != null) {
                    onDismiss.call(this);
                }
            });
        } else {
            Anime.cancelAll(contents, tooltip);
            dialog.dismiss();
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
