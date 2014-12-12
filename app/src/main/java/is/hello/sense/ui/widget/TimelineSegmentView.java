package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.common.Styles;

public final class TimelineSegmentView extends View {
    private int leftInset;
    private int rightInset;
    private int stripeWidth;
    private int stripeCornerRadius;

    private final Paint textPaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Path stripePath = new Path();
    private final Paint stripePaint = new Paint();
    private final RectF arcRect = new RectF();

    private Drawable eventImage;
    private String text;
    private int sleepDepth;
    private boolean stripeTopRounded = false;
    private boolean stripeBottomRounded = false;

    public TimelineSegmentView(Context context) {
        super(context);
        initialize(null, 0);
    }

    public TimelineSegmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    public TimelineSegmentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs, defStyleAttr);
    }

    private void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        Resources resources = getResources();

        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setTextSize(resources.getDimensionPixelOffset(R.dimen.text_size_body));
        textPaint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), Styles.TYPEFACE_LIGHT));

        stripePaint.setAntiAlias(true);

        this.leftInset = resources.getDimensionPixelSize(R.dimen.widget_timeline_segment_left_inset);
        this.rightInset = resources.getDimensionPixelSize(R.dimen.widget_timeline_segment_right_inset);
        this.stripeWidth = resources.getDimensionPixelSize(R.dimen.event_stripe_width);
        this.stripeCornerRadius = resources.getDimensionPixelSize(R.dimen.widget_timeline_segment_corner_radius);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        stripePath.reset();

        int width = canvas.getWidth() - (leftInset + rightInset);
        int height = canvas.getHeight();
        int minX = leftInset;
        int minY = 0, maxY = minY + height;

        float stripeCap = stripeWidth / 2f;
        float stripeMaxX = minX + stripeWidth;

        if (stripeTopRounded) {
            stripePath.moveTo(minX, minY + stripeCap);
            arcRect.set(minX, minY, stripeMaxX, minY + stripeCap);
            stripePath.arcTo(arcRect, 180f, 180f);
        } else {
            stripePath.moveTo(minX, minY);
            stripePath.lineTo(minX + stripeWidth, minY);
        }

        if (stripeBottomRounded) {
            stripePath.lineTo(stripeMaxX, maxY - stripeCap);
            arcRect.set(minX, maxY - stripeCap, stripeMaxX, maxY);
            stripePath.arcTo(arcRect, 0f, 180f);
            stripePath.lineTo(minX, minY + stripeCap);
        } else {
            stripePath.lineTo(stripeMaxX, maxY);
            stripePath.lineTo(minX, maxY);
            stripePath.lineTo(minX, minY);
        }

        canvas.drawPath(stripePath, stripePaint);
    }


    //region Attributes

    public void setEventImage(Drawable eventImage) {
        this.eventImage = eventImage;
        invalidate();
    }

    public void setEventImage(@DrawableRes int eventImageRes) {
        setEventImage(getResources().getDrawable(eventImageRes));
    }

    public void setText(String text) {
        this.text = text;
        invalidate();
    }

    public void setText(@StringRes int stringRes) {
        setText(getResources().getString(stringRes));
    }

    public void setSleepDepth(int sleepDepth) {
        this.sleepDepth = sleepDepth;
        fillPaint.setColor(Styles.getSleepDepthDimmedColorRes(sleepDepth));
        stripePaint.setColor(Styles.getSleepDepthColorRes(sleepDepth));
        invalidate();
    }

    public void setStripeTopRounded(boolean stripeTopRounded) {
        this.stripeTopRounded = stripeTopRounded;
        invalidate();
    }

    public void setStripeBottomRounded(boolean stripeBottomRounded) {
        this.stripeBottomRounded = stripeBottomRounded;
        invalidate();
    }

    //endregion
}

