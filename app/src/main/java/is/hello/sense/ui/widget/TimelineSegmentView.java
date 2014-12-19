package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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
    private float ovalInset;
    private float textLeftInset;
    private float[] topRadii;
    private float[] bottomRadii;

    //endregion


    //region Drawing Structures

    private final RectF rect = new RectF();
    private final Rect textBounds = new Rect();
    private final Paint textPaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Path stripePath = new Path();
    private final Paint stripePaint = new Paint();
    private final Paint eventOvalPaint = new Paint();

    //endregion


    //region Properties

    private boolean hasText = false;

    private Drawable eventDrawable;
    private String text;
    private int sleepDepth;
    private StripeRounding stripeRounding = StripeRounding.NONE;

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

        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_body));
        textPaint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), Styles.TYPEFACE_LIGHT));

        stripePaint.setAntiAlias(true);
        eventOvalPaint.setAntiAlias(true);
        eventOvalPaint.setColor(Color.WHITE);

        this.leftInset = resources.getDimension(R.dimen.view_timeline_segment_left_inset);
        this.rightInset = resources.getDimension(R.dimen.view_timeline_segment_right_inset);
        this.stripeWidth = resources.getDimension(R.dimen.view_timeline_segment_stripe_width);
        this.ovalInset = resources.getDimension(R.dimen.view_timeline_segment_oval_inset);
        this.textLeftInset = resources.getDimension(R.dimen.view_timeline_segment_left_inset);

        float stripeCornerRadius = resources.getDimension(R.dimen.view_timeline_segment_corner_radius);
        this.topRadii = new float[] {
                stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
                0f, 0f, 0f, 0f,
        };
        this.bottomRadii = new float[] {
                0f, 0f, 0f, 0f,
                stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
        };
    }

    //endregion


    @Override
    protected void onDraw(Canvas canvas) {
        float width = canvas.getWidth() - (leftInset + rightInset);
        float height = canvas.getHeight();
        float minX = leftInset;
        float minY = 0,
              midY = (minY + height) / 2,
              maxY = minY + height;


        //region Stripe Path

        stripePath.reset();

        float stripeMaxX = minX + stripeWidth;
        rect.set(minX, minY, stripeMaxX, maxY);
        switch (stripeRounding) {
            case NONE: {
                stripePath.addRect(rect, Path.Direction.CW);
                break;
            }

            case TOP: {
                stripePath.addRoundRect(rect, topRadii, Path.Direction.CW);
                break;
            }

            case BOTTOM: {
                stripePath.addRoundRect(rect, bottomRadii, Path.Direction.CW);
                break;
            }
        }

        //endregion


        //region Stripe + Background Fills

        float stripeMidPoint = (float) Math.ceil(stripeWidth / 2f);
        float percentage = sleepDepth / 100f;
        float fillWidth = (width - stripeMidPoint) * percentage;
        canvas.drawRect(minX + stripeMidPoint, minY, minX + fillWidth, maxY, fillPaint);

        canvas.drawPath(stripePath, stripePaint);

        //endregion


        //region Event Icon

        if (eventDrawable != null) {
            float stripeMidX = (int) (minX + stripeMidPoint);
            float drawableWidth = eventDrawable.getIntrinsicWidth();
            float drawableHeight = eventDrawable.getIntrinsicHeight();

            rect.set(minX, midY - stripeMidPoint, stripeMaxX, midY + stripeMidPoint);
            rect.inset(ovalInset, ovalInset);
            canvas.drawOval(rect, eventOvalPaint);

            eventDrawable.setBounds(
                    Math.round(stripeMidX - drawableWidth / 2f),
                    Math.round(midY - drawableHeight / 2f),
                    Math.round(stripeMidX + drawableWidth / 2f),
                    Math.round(midY + drawableHeight / 2f)
            );
            eventDrawable.draw(canvas);
        }

        //endregion


        //region Text

        if (hasText) {
            float textX = minX + stripeWidth + textLeftInset;
            float textY = midY - textBounds.centerY();
            canvas.drawText(text, textX, textY, textPaint);
        }

        //endregion
    }


    //region Attributes

    public void setEventDrawable(@Nullable Drawable eventDrawable) {
        this.eventDrawable = eventDrawable;
        invalidate();
    }

    public void setEventResource(@DrawableRes int eventImageRes) {
        setEventDrawable(getResources().getDrawable(eventImageRes));
    }

    public void setText(@Nullable String text) {
        this.text = text;
        this.hasText = !TextUtils.isEmpty(text);

        if (hasText) {
            textPaint.getTextBounds(text, 0, text.length(), textBounds);
        } else {
            textBounds.set(0, 0, 0, 0);
        }

        invalidate();
    }

    public void setText(@StringRes int stringRes) {
        setText(getResources().getString(stringRes));
    }

    public void setSleepDepth(int sleepDepth) {
        this.sleepDepth = sleepDepth;

        Resources resources = getResources();
        fillPaint.setColor(resources.getColor(Styles.getSleepDepthDimmedColorRes(sleepDepth)));
        stripePaint.setColor(resources.getColor(Styles.getSleepDepthColorRes(sleepDepth)));

        invalidate();
    }

    public void setStripeRounding(StripeRounding stripeRounding) {
        this.stripeRounding = stripeRounding;
        invalidate();
    }

    //endregion


    public static enum StripeRounding {
        NONE,
        TOP,
        BOTTOM,
    }
}

