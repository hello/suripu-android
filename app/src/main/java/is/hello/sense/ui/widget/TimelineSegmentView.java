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

    private final float leftInset;
    private final float rightInset;
    private final float stripeWidth;

    private final int textSideInset;
    private final int leftUnderlineColor;
    private final int leftTextColor;
    private final int rightUnderlineColor;
    private final int rightTextColor;

    //endregion


    //region Drawing Structures

    private final RectF fillRect = new RectF();
    private final Paint fillPaint = new Paint();
    private final Paint stripePaint = new Paint();

    private final Rect timeTextRect = new Rect();
    private final Paint timePaint = new Paint();
    private final Paint timeStrokePaint = new Paint();
    private final Path timeStrokePath = new Path();

    //endregion


    //region Properties

    private Drawable eventDrawable;
    private int sleepDepth;
    private @Nullable String leftTime;
    private @Nullable String rightTime;

    //endregion


    //region Creation

    public TimelineSegmentView(Context context) {
        this(context, null);
    }

    public TimelineSegmentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineSegmentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources resources = getResources();

        stripePaint.setAntiAlias(true);
        stripePaint.setColor(resources.getColor(R.color.timeline_segment_stripe));

        timePaint.setAntiAlias(true);
        timePaint.setSubpixelText(true);
        timePaint.setTextSize(resources.getDimensionPixelSize(R.dimen.text_size_timeline_time));

        float strokeGap = resources.getDimension(R.dimen.view_timeline_segment_stroke_gap);
        timeStrokePaint.setStyle(Paint.Style.STROKE);
        timeStrokePaint.setStrokeWidth(resources.getDimension(R.dimen.divider_size));
        timeStrokePaint.setPathEffect(new DashPathEffect(new float[]{strokeGap, strokeGap}, 0));

        this.leftInset = resources.getDimension(R.dimen.view_timeline_segment_left_inset);
        this.rightInset = resources.getDimension(R.dimen.view_timeline_segment_right_inset);
        this.stripeWidth = resources.getDimension(R.dimen.view_timeline_segment_stripe_width);

        this.textSideInset = resources.getDimensionPixelSize(R.dimen.view_timeline_segment_text_inset);

        this.leftUnderlineColor = resources.getColor(R.color.timeline_segment_underline_left);
        this.leftTextColor = resources.getColor(R.color.timeline_segment_text_left);

        this.rightUnderlineColor = resources.getColor(R.color.timeline_segment_underline_right);
        this.rightTextColor = resources.getColor(R.color.timeline_segment_text_right);
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
        fillRect.set(midX - fillWidth / 2f, minY, midX + fillWidth / 2f, maxY);
        canvas.drawRect(fillRect, fillPaint);
        canvas.drawRect(midX - stripeWidth / 2f, minY, midX + stripeWidth / 2f, maxY, stripePaint);

        //endregion


        //region Times

        if (!TextUtils.isEmpty(leftTime)) {
            timeStrokePath.reset();
            timePaint.getTextBounds(leftTime, 0, leftTime.length(), timeTextRect);

            timeStrokePaint.setColor(leftUnderlineColor);
            timePaint.setColor(leftTextColor);

            float textX = textSideInset;
            float textY = Math.round(midY + timeTextRect.centerY());

            float lineY = textY + timeTextRect.height();
            timeStrokePath.moveTo(minX - leftInset, lineY);
            timeStrokePath.lineTo(midX, lineY);

            canvas.drawPath(timeStrokePath, timeStrokePaint);
            canvas.drawText(leftTime, textX, textY, timePaint);
        }

        if (!TextUtils.isEmpty(rightTime)) {
            timeStrokePath.reset();
            timePaint.getTextBounds(rightTime, 0, rightTime.length(), timeTextRect);

            timeStrokePaint.setColor(rightUnderlineColor);
            timePaint.setColor(rightTextColor);

            timeStrokePath.moveTo(midX, midY);
            timeStrokePath.lineTo(maxX + rightInset, midY);

            float textX = Math.round(canvas.getWidth() - textSideInset - timeTextRect.width());
            float textY = Math.round(midY - timeTextRect.height());

            canvas.drawPath(timeStrokePath, timeStrokePaint);
            canvas.drawText(rightTime, textX, textY, timePaint);
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

    public void setSleepDepth(int sleepDepth, boolean dimmed) {
        this.sleepDepth = sleepDepth;

        Resources resources = getResources();
        int colorRes = Styles.getSleepDepthColorRes(sleepDepth, dimmed);
        fillPaint.setColor(resources.getColor(colorRes));

        invalidate();
    }

    public void setLeftTime(@Nullable String leftTime) {
        this.leftTime = leftTime;
        invalidate();
    }

    public void setRightTime(@Nullable String rightTime) {
        this.rightTime = rightTime;
        invalidate();
    }

    //endregion
}

