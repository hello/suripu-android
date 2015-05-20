package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.FragmentPageView.Position;

public final class FragmentPageTitleStrip extends FrameLayout implements FragmentPageView.Decor {
    private final TextView textView1;
    private final TextView textView2;

    private boolean textViewsSwapped = false;
    private Position swipePosition;

    //region Lifecycle

    public FragmentPageTitleStrip(@NonNull Context context) {
        this(context, null);
    }

    public FragmentPageTitleStrip(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FragmentPageTitleStrip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        this.textView1 = createTextView();
        textView1.setBackgroundColor(Color.RED);
        addView(textView1, layoutParams);

        this.textView2 = createTextView();
        textView2.setVisibility(INVISIBLE);
        textView2.setBackgroundColor(Color.BLUE);
        addView(textView2, layoutParams);
    }

    //endregion


    //region Internal Views

    private TextView createTextView() {
        TextView textView = new TextView(getContext());
        textView.setTextAppearance(getContext(), R.style.AppTheme_Text_ScreenTitle);
        textView.setGravity(Gravity.CENTER);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setSingleLine();
        return textView;
    }

    private TextView getForegroundTextView() {
        if (textViewsSwapped) {
            return textView2;
        } else {
            return textView1;
        }
    }

    private TextView getBackgroundTextView() {
        if (textViewsSwapped) {
            return textView1;
        } else {
            return textView2;
        }
    }

    private void swapTextViews() {
        this.textViewsSwapped = !textViewsSwapped;
        getBackgroundTextView().setVisibility(INVISIBLE);
        invalidate();
    }

    //endregion


    //region Attributes

    public void setDimmed(boolean dimmed) {

    }

    //endregion


    //region Decor

    @Override
    public void onInteractionBegan() {
        this.swipePosition = null;
    }

    @Override
    public void onInteractionUpdated(float amount) {
        View background = getBackgroundTextView(),
             foreground = getForegroundTextView();

        if (swipePosition == null) {
            background.setVisibility(VISIBLE);
        }

        this.swipePosition = amount > 0.0 ? Position.BEFORE : Position.AFTER;


        float foregroundX = getMeasuredWidth() * amount;
        foreground.setX(foregroundX);

        float backgroundX;
        if (amount > 0f) {
            backgroundX = foregroundX - background.getMeasuredWidth();
        } else {
            backgroundX = foregroundX + background.getMeasuredWidth();
        }
        background.setX(backgroundX);
    }

    @Override
    public void onInteractionSnapBack(long duration, @NonNull AnimatorConfig animatorConfig, @Nullable AnimatorContext animatorContext) {
        if (animatorContext == null) {
            return;
        }

        animatorContext.transaction(animatorConfig.withDuration(duration), 0, f -> {
            float viewWidth = getMeasuredWidth();
            f.animate(getForegroundTextView())
             .x(0f);

            f.animate(getBackgroundTextView())
             .x(-viewWidth);
        }, finished -> {
            if (!finished) {
                return;
            }

            this.swipePosition = null;
        });
    }

    @Override
    public void onInteractionConcluded(long duration, @NonNull AnimatorConfig animatorConfig, @Nullable AnimatorContext animatorContext) {
        if (animatorContext == null) {
            return;
        }

        animatorContext.transaction(animatorConfig.withDuration(duration), 0, f -> {
            float viewWidth = getMeasuredWidth();
            f.animate(getForegroundTextView())
             .x(swipePosition == Position.BEFORE ? viewWidth : -viewWidth);

            f.animate(getBackgroundTextView())
             .x(0f);
        }, finished -> {
            if (!finished) {
                return;
            }

            swapTextViews();
            this.swipePosition = null;
        });
    }

    @Override
    public void onInteractionConclusionInterrupted() {
        PropertyAnimatorProxy.stop(textView1, textView2);
    }

    //endregion
}
