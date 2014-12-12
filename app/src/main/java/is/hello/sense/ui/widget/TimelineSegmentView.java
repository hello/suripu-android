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
import is.hello.sense.ui.common.Styles;

public final class TimelineSegmentView extends View {
    private int leftInset;
    private int rightInset;
    private int stripeWidth;
    private int ovalInset;
    private int textLeftInset;
    private float[] topRadii;
    private float[] bottomRadii;

    private final RectF rect = new RectF();
    private final Rect textBounds = new Rect();
    private final Paint textPaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Path stripePath = new Path();
    private final Paint stripePaint = new Paint();
    private final Paint eventOvalPaint = new Paint();

    private boolean hasText = false;

    private Drawable eventDrawable;
    private String text;
    private int sleepDepth;
    private StripeRounding stripeRounding = StripeRounding.NONE;

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

        this.leftInset = resources.getDimensionPixelSize(R.dimen.widget_timeline_segment_left_inset);
        this.rightInset = resources.getDimensionPixelSize(R.dimen.widget_timeline_segment_right_inset);
        this.stripeWidth = resources.getDimensionPixelSize(R.dimen.event_stripe_width);
        this.ovalInset = resources.getDimensionPixelSize(R.dimen.widget_timeline_segment_oval_inset);
        this.textLeftInset = resources.getDimensionPixelOffset(R.dimen.widget_timeline_segment_left_inset);

        int stripeCornerRadius = resources.getDimensionPixelSize(R.dimen.widget_timeline_segment_corner_radius);
        this.topRadii = new float[] {
                stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
                0f, 0f, 0f, 0f,
        };
        this.bottomRadii = new float[] {
                0f, 0f, 0f, 0f,
                stripeCornerRadius, stripeCornerRadius, stripeCornerRadius, stripeCornerRadius,
        };
    }


    @Override
    protected void onDraw(Canvas canvas) {
        stripePath.reset();

        int width = canvas.getWidth() - (leftInset + rightInset);
        int height = canvas.getHeight();
        int minX = leftInset;
        int minY = 0, midY = (minY + height) / 2, maxY = minY + height;

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

        float stripeMidPoint = (float) Math.ceil(stripeWidth / 2f);
        float percentage = sleepDepth / 100f;
        float fillWidth = (width - stripeMidPoint) * percentage;
        canvas.drawRect(minX + stripeMidPoint, minY, minX + fillWidth, maxY, fillPaint);

        canvas.drawPath(stripePath, stripePaint);

        if (eventDrawable != null) {
            int stripeMidX = (int) (minX + stripeMidPoint);
            int drawableWidth = eventDrawable.getIntrinsicWidth();
            int drawableHeight = eventDrawable.getIntrinsicHeight();

            rect.set(minX, midY - stripeWidth / 2, stripeMaxX, midY + stripeWidth / 2);
            rect.inset(ovalInset, ovalInset);
            canvas.drawOval(rect, eventOvalPaint);

            eventDrawable.setBounds(
                    stripeMidX - drawableWidth / 2,
                    midY - drawableHeight / 2,
                    stripeMidX + drawableWidth / 2,
                    midY + drawableHeight / 2
            );
            eventDrawable.draw(canvas);
        }

        if (hasText) {
            float textX = minX + stripeWidth + textLeftInset;
            float textY = midY - textBounds.centerY();
            canvas.drawText(text, textX, textY, textPaint);
        }
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

