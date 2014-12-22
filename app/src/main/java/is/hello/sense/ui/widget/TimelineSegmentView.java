package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

/**
 * Draws a single segment of a user's timeline. This includes the segment's
 * score level, event icon, and event message. This class replaces a regular
 * inflated layout for improved scroll performance.
 */
public final class TimelineSegmentView extends View {
    //region Drawing Constants

    private float leftInset;
    private float rightInset;
    private float stripeWidth;
    private float cornerRadius;

    //endregion


    //region Drawing Structures

    private final RectF rect = new RectF();
    private final Paint fillPaint = new Paint();
    private final Paint stripePaint = new Paint();

    //endregion


    //region Properties

    private Drawable eventDrawable;
    private int sleepDepth;
    private StripeInset stripeInset = StripeInset.NONE;
    private boolean rounded;

    //endregion


    //region Creation

    public TimelineSegmentView(Context context) {
        super(context);
        initialize();
    }

    public TimelineSegmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public TimelineSegmentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        Resources resources = getResources();

        stripePaint.setAntiAlias(true);
        stripePaint.setColor(resources.getColor(R.color.timeline_segment_stripe));

        this.leftInset = resources.getDimension(R.dimen.view_timeline_segment_left_inset);
        this.rightInset = resources.getDimension(R.dimen.view_timeline_segment_right_inset);
        this.stripeWidth = resources.getDimension(R.dimen.view_timeline_segment_stripe_width);

        this.cornerRadius = resources.getDimension(R.dimen.view_timeline_segment_corner_radius);
    }

    //endregion


    @Override
    protected void onDraw(Canvas canvas) {
        float width = canvas.getWidth() - (leftInset + rightInset);
        float height = canvas.getHeight();
        float minX = leftInset;
        float minY = 0f,
              midY = (minY + height) / 2f,
              midX = minX + (width / 2f),
              maxY = minY + height;


        //region Stripe + Background Fills

        float percentage = sleepDepth / 100f;
        float fillWidth = width * percentage;
        rect.set(midX - fillWidth / 2f, minY, midX + fillWidth / 2f, maxY);
        if (rounded) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
        } else {
            canvas.drawRect(rect, fillPaint);
        }


        float stripeTop = minY,
              stripeBottom = maxY;

        switch (stripeInset) {
            case TOP: {
                stripeTop += height / 2f;
                break;
            }

            case BOTTOM: {
                stripeBottom -= height / 2f;
                break;
            }

            case NONE:
            default: {
                break;
            }
        }

        canvas.drawRect(midX - stripeWidth / 2f, stripeTop, midX + stripeWidth / 2f, stripeBottom, stripePaint);

        //endregion


        //region Event Icon

        if (eventDrawable != null) {
            float drawableWidth = eventDrawable.getIntrinsicWidth();
            float drawableHeight = eventDrawable.getIntrinsicHeight();

            eventDrawable.setBounds(
                    Math.round(midX - drawableWidth / 2f),
                    Math.round(midY - drawableHeight / 2f),
                    Math.round(midX + drawableWidth / 2f),
                    Math.round(midY + drawableHeight / 2f)
            );
            eventDrawable.draw(canvas);
        }

        //endregion
    }


    //region Attributes

    public void setEventDrawable(@Nullable Drawable eventDrawable) {
        this.eventDrawable = eventDrawable;
        invalidate();
    }

    public void setEventResource(@DrawableRes int eventImageRes) {
        if (eventImageRes == 0) {
            setEventDrawable(null);
        } else {
            setEventDrawable(getResources().getDrawable(eventImageRes));
        }
    }

    public void setSleepDepth(int sleepDepth) {
        this.sleepDepth = sleepDepth;

        Resources resources = getResources();
        fillPaint.setColor(resources.getColor(Styles.getSleepDepthDimmedColorRes(sleepDepth)));

        invalidate();
    }

    public void setStripeInset(StripeInset stripeInset) {
        this.stripeInset = stripeInset;
        invalidate();
    }

    public void setRounded(boolean rounded) {
        this.rounded = rounded;
        invalidate();
    }

    //endregion


    public static enum StripeInset {
        NONE,
        TOP,
        BOTTOM,
    }
}

