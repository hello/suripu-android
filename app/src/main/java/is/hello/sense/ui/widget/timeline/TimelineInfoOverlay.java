package is.hello.sense.ui.widget.timeline;

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
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
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
    private final int backgroundColor;
    private final Activity activity;
    private final AnimatorContext animatorContext;

    private final Handler delayHandler = new Handler(Looper.getMainLooper(), this);
    private final Resources resources;
    private final Dialog dialog;

    private final FrameLayout contents;
    private final TextView tooltip;
    private final float maxBackgroundWidthFraction;

    private int darkenOverlayColor = Color.TRANSPARENT;
    private float overlaySleepDepthPercentage;

    private @Nullable Action1<TimelineInfoOverlay> onDismiss;

    public TimelineInfoOverlay(@NonNull final Activity activity,
                               @NonNull final AnimatorContext animatorContext) {
        this.activity = activity;
        this.animatorContext = animatorContext;

        this.resources = activity.getResources();
        this.dialog = new Dialog(activity, R.style.AppTheme_Dialog_FullScreen);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        dialog.getWindow().setWindowAnimations(R.style.WindowAnimations);
        dialog.setCancelable(true);

        this.contents = new FrameLayout(activity);
        dialog.setContentView(contents);

        this.tooltip = (TextView) activity.getLayoutInflater().inflate(R.layout.timeline_tooltip_textview, contents, false);
        contents.addView(tooltip);

        this.backgroundColor = ContextCompat.getColor(activity, R.color.background_light_overlay);
        final TypedValue typedValue = new TypedValue();
        resources.getValue(R.dimen.timeline_segment_item_end_max_fraction, typedValue, true);
        this.maxBackgroundWidthFraction = typedValue.getFloat();
    }

    public void bindEvent(@NonNull final TimelineEvent event) {
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

    public void setOnDismiss(@Nullable final Action1<TimelineInfoOverlay> onDismiss) {
        this.onDismiss = onDismiss;
    }

    /**
     * @param screenSize define total available device width and height
     * @param backgroundFrame define overlay boundaries
     * @param focusedFrame define focused segment boundaries
     * @return drawable with background overlays applied
     */
    private Drawable createBackground(@NonNull final Point screenSize,
                                      @NonNull final Rect backgroundFrame,
                                      @NonNull final Rect focusedFrame) {
        final Path backgroundPath = new Path();

        final int contentRight = Math.round(backgroundFrame.right * maxBackgroundWidthFraction);
        //from top of screen to top of view
        backgroundPath.addRect(backgroundFrame.left, backgroundFrame.top,
                               backgroundFrame.right, focusedFrame.top,
                               Path.Direction.CW);

        //from top of view to bottom of view
        backgroundPath.addRect(contentRight, focusedFrame.top,
                               backgroundFrame.right, focusedFrame.bottom,
                               Path.Direction.CW);
        //from bottom of view to bottom of screen
        backgroundPath.addRect(backgroundFrame.left, focusedFrame.bottom,
                               backgroundFrame.right, backgroundFrame.bottom,
                               Path.Direction.CW);

        final ShapeDrawable background = new ShapeDrawable(new PathShape(backgroundPath,
                                                                         screenSize.x,
                                                                         screenSize.y));
        background.getPaint()
                  .setColor(backgroundColor);

        if (darkenOverlayColor == Color.TRANSPARENT) {
            return background;
        } else {
            final Path overlayPath = new Path();
            //overlay for focused segment
            overlayPath.addRect(backgroundFrame.left,
                                focusedFrame.top,
                                contentRight * overlaySleepDepthPercentage,
                                focusedFrame.bottom,
                                Path.Direction.CW);

            final ShapeDrawable overlay = new ShapeDrawable(new PathShape(overlayPath,
                                                                          screenSize.x,
                                                                          screenSize.y));
            overlay.getPaint().setColor(darkenOverlayColor);

            final Drawable[] layers = {background, overlay};
            return new LayerDrawable(layers);
        }
    }

    public void show(@NonNull final View fromView,
                     @IdRes final int backgroundRootResId,
                     final boolean animate) {
        if (dialog.isShowing()) {
            return;
        }

        final Window wm = activity.getWindow();
        if(wm == null) {
            return;
        }
        final Point screenSize = new Point();
        wm.getWindowManager().getDefaultDisplay().getSize(screenSize);

        final Rect backgroundFrame = new Rect();
        final View backgroundView = activity.findViewById(backgroundRootResId);
        backgroundView.getGlobalVisibleRect(backgroundFrame);

        final Rect viewFrame = new Rect();
        fromView.getGlobalVisibleRect(viewFrame);
        //todo needs debugging here because alignment is off when not center of screen
        viewFrame.top -= backgroundFrame.top /2;
        viewFrame.bottom -= backgroundFrame.top /2;

        backgroundFrame.top = 0;

        contents.setBackground(createBackground(screenSize,
                                                backgroundFrame,
                                                viewFrame));

        final LayoutParams layoutParams = (FrameLayout.LayoutParams) tooltip.getLayoutParams();
        final int tooltipBottomMargin = layoutParams.bottomMargin;
        layoutParams.bottomMargin += (screenSize.y - viewFrame.top);
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

    public void dismiss(final boolean animate) {
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
    public boolean handleMessage(final Message msg) {
        if (msg.what == MSG_DISMISS) {
            dismiss(true);
            return true;
        }

        return false;
    }
}
