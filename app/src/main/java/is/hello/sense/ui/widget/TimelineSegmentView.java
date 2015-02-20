package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

/**
 * Draws a single segment of a user's timeline. This includes the segment's
 * score level, event icon, and event message. This class replaces a regular
 * inflated layout for improved scroll performance.
 */
public final class TimelineSegmentView extends View {
    private final Invariants invariants;

    //region Drawing Structures

    private final RectF fillRect = new RectF();
    private final Paint fillPaint = new Paint();
    private final Paint stripePaint = new Paint();

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

    public TimelineSegmentView(@NonNull Context context) {
        this(context, new Invariants(context.getResources()));
    }

    public TimelineSegmentView(@NonNull Context context, @NonNull Invariants invariants) {
        super(context);

        this.invariants = invariants;

        Resources resources = context.getResources();
        float strokeGap = resources.getDimension(R.dimen.view_timeline_segment_stroke_gap);
        timeStrokePaint.setStyle(Paint.Style.STROKE);
        timeStrokePaint.setStrokeWidth(resources.getDimension(R.dimen.divider_size));
        timeStrokePaint.setPathEffect(new DashPathEffect(new float[]{strokeGap, strokeGap}, 0));
    }

    //endregion


    @Override
    protected void onDraw(Canvas canvas) {
        float width = canvas.getWidth() - (invariants.leftInset + invariants.rightInset);
        float height = canvas.getHeight();
        float minX = invariants.leftInset;
        float minY = 0f,
              midY = (minY + height) / 2f,
              midX = minX + (width / 2f),
              maxY = minY + height;


        //region Stripe + Background Fills

        float percentage = sleepDepth / 100f;
        float fillWidth = (width - invariants.leftInset - invariants.rightInset) * percentage;
        fillRect.set(midX - fillWidth / 2f, minY, midX + fillWidth / 2f, maxY);
        canvas.drawRect(fillRect, fillPaint);
        canvas.drawRect(midX - invariants.stripeWidth / 2f, minY, midX + invariants.stripeWidth / 2f, maxY, stripePaint);

        //endregion


        //region Times

        if (!TextUtils.isEmpty(leftTime)) {
            timeStrokePaint.setColor(invariants.leftUnderlineColor);

            timeStrokePath.reset();
            timeStrokePath.moveTo(0, midY);
            timeStrokePath.lineTo(midX, midY);

            canvas.drawPath(timeStrokePath, timeStrokePaint);

            float textX = invariants.textSideInset;
            float textY = midY + invariants.textLineHeight;
            canvas.drawText(leftTime, textX, textY, invariants.leftTimePaint);
        }

        if (!TextUtils.isEmpty(rightTime)) {
            timeStrokePaint.setColor(invariants.rightUnderlineColor);

            timeStrokePath.reset();
            timeStrokePath.moveTo(canvas.getWidth(), midY);
            timeStrokePath.lineTo(midX, midY);

            canvas.drawPath(timeStrokePath, timeStrokePaint);

            float textX = canvas.getWidth() - invariants.textSideInset;
            float textY = midY + invariants.textLineHeight;
            canvas.drawText(rightTime, textX, textY, invariants.rightTimePaint);
        }


        //endregion


        //region Event Icon

        if (eventDrawable != null) {
            float drawableWidth = eventDrawable.getIntrinsicWidth();
            float drawableMinY = minY - invariants.imageShadow;
            float drawableMaxY = drawableMinY + eventDrawable.getIntrinsicHeight();

            eventDrawable.setBounds(
                Math.round(midX - drawableWidth / 2f),
                Math.round(drawableMinY),
                Math.round(midX + drawableWidth / 2f),
                Math.round(drawableMaxY)
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
        if (dimmed) {
            stripePaint.setColor(resources.getColor(R.color.timeline_segment_stripe_dimmed));
        } else {
            stripePaint.setColor(resources.getColor(R.color.timeline_segment_stripe));
        }

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


    /**
     * Contains most of the structures necessary to draw a timeline segment that
     * can be shared between instances used within the same adapter.
     */
    public static class Invariants {
        public final float leftInset;
        public final float rightInset;
        public final float stripeWidth;

        public final int textSideInset;
        public final int imageShadow;
        public final int textLineHeight;

        public final Paint leftTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        public final Paint rightTimePaint;

        public final int leftUnderlineColor;
        public final int rightUnderlineColor;

        public Invariants(@NonNull Resources resources) {
            this.leftInset = resources.getDimension(R.dimen.view_timeline_segment_left_inset);
            this.rightInset = resources.getDimension(R.dimen.view_timeline_segment_right_inset);
            this.stripeWidth = resources.getDimension(R.dimen.view_timeline_segment_stripe_width);

            this.textSideInset = resources.getDimensionPixelSize(R.dimen.view_timeline_segment_text_inset);
            this.imageShadow = resources.getDimensionPixelSize(R.dimen.timeline_segment_event_image_shadow);

            leftTimePaint.setColor(resources.getColor(R.color.timeline_segment_text_left));
            leftTimePaint.setTextSize(resources.getDimensionPixelSize(R.dimen.text_size_timeline_time));
            leftTimePaint.setTextAlign(Paint.Align.LEFT);

            this.rightTimePaint = new Paint(leftTimePaint);
            rightTimePaint.setColor(resources.getColor(R.color.timeline_segment_text_right));
            rightTimePaint.setTextAlign(Paint.Align.RIGHT);

            Paint.FontMetricsInt fontMetrics = leftTimePaint.getFontMetricsInt();
            this.textLineHeight = fontMetrics.top + fontMetrics.descent;

            this.leftUnderlineColor = resources.getColor(R.color.timeline_segment_underline_left);
            this.rightUnderlineColor = resources.getColor(R.color.timeline_segment_underline_right);
        }
    }
}

