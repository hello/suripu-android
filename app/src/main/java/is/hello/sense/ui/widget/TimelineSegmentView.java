package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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

    private int leftUnderlineColor;
    private int leftTimestampColor;
    private int rightUnderlineColor;
    private int rightTimestampColor;

    //endregion


    //region Drawing Structures

    private final RectF rect = new RectF();
    private final Paint fillPaint = new Paint();
    private final Paint stripePaint = new Paint();

    private final Rect timestampTextRect = new Rect();
    private final Paint timestampPaint = new Paint();
    private final Paint timestampStrokePaint = new Paint();
    private final Path timestampStrokePath = new Path();

    //endregion


    //region Properties

    private Drawable eventDrawable;
    private int sleepDepth;
    private TimestampSide timestampSide = TimestampSide.RIGHT;
    private @Nullable String timestampString;

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

        timestampPaint.setAntiAlias(true);
        timestampPaint.setSubpixelText(true);
        timestampPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.text_size_timeline_time));

        float strokeGap = resources.getDimension(R.dimen.view_timeline_segment_stroke_gap);
        timestampStrokePaint.setStyle(Paint.Style.STROKE);
        timestampStrokePaint.setStrokeWidth(resources.getDimension(R.dimen.divider_size));
        timestampStrokePaint.setPathEffect(new DashPathEffect(new float[] { strokeGap, strokeGap }, 0));

        this.leftInset = resources.getDimension(R.dimen.view_timeline_segment_left_inset);
        this.rightInset = resources.getDimension(R.dimen.view_timeline_segment_right_inset);
        this.stripeWidth = resources.getDimension(R.dimen.view_timeline_segment_stripe_width);

        this.leftUnderlineColor = resources.getColor(R.color.timeline_segment_underline_left);
        this.leftTimestampColor = resources.getColor(R.color.text_dim);

        this.rightUnderlineColor = resources.getColor(R.color.timeline_segment_underline_right);
        this.rightTimestampColor = resources.getColor(R.color.light_accent);
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
              maxX = minX + width,
              maxY = minY + height;


        //region Stripe + Background Fills

        float percentage = sleepDepth / 100f;
        float fillWidth = (width - leftInset - rightInset) * percentage;
        rect.set(midX - fillWidth / 2f, minY, midX + fillWidth / 2f, maxY);
        canvas.drawRect(rect, fillPaint);
        canvas.drawRect(midX - stripeWidth / 2f, minY, midX + stripeWidth / 2f, maxY, stripePaint);

        //endregion


        //region Timestamp

        if (timestampString != null) {
            timestampStrokePath.reset();
            timestampPaint.getTextBounds(timestampString, 0, timestampString.length(), timestampTextRect);

            float textX, textY;
            if (timestampSide == TimestampSide.RIGHT) {
                timestampStrokePaint.setColor(rightUnderlineColor);
                timestampPaint.setColor(rightTimestampColor);

                timestampStrokePath.moveTo(midX, midY);
                timestampStrokePath.lineTo(maxX + rightInset, midY);

                textX = Math.round(maxX - timestampTextRect.width());
                textY = Math.round(midY - timestampTextRect.height());
            } else {
                timestampStrokePaint.setColor(leftUnderlineColor);
                timestampPaint.setColor(leftTimestampColor);

                textX = minX;
                textY = Math.round(midY + timestampTextRect.height() / 2f);

                float lineY = textY + timestampTextRect.height() / 2f;
                timestampStrokePath.moveTo(minX - leftInset, lineY);
                timestampStrokePath.lineTo(midX, lineY);
            }

            canvas.drawPath(timestampStrokePath, timestampStrokePaint);
            canvas.drawText(timestampString, textX, textY, timestampPaint);
        }

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

    public void setTimestampString(@Nullable String timestampString, @Nullable TimestampSide side) {
        if (!TextUtils.equals(timestampString, this.timestampString)) {
            this.timestampString = timestampString;
            this.timestampSide = side;
            invalidate();
        }
    }

    //endregion


    public static enum TimestampSide {
        LEFT,
        RIGHT,
    }
}

