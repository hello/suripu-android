package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import is.hello.sense.R;

@SuppressWarnings("UnusedDeclaration")
public final class PieGraphView extends GraphView {
    private final Paint paint = new Paint();
    private final Path piePath = new Path();
    private final RectF arcRect = new RectF();
    private final RectF clipRect = new RectF();

    private float displayScaleFactor;
    private int trackColor;
    private int centerColor;

    public PieGraphView(Context context) {
        super(context);
    }

    public PieGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieGraphView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        piePath.reset();
        paint.reset();
        paint.setAntiAlias(true);

        int width = canvas.getWidth(), height = canvas.getHeight();
        float scale = ((float) value / (float) maxValue);
        piePath.moveTo(width / 2f, height / 2f);

        arcRect.set(0f, 0f, width, height);
        piePath.arcTo(arcRect, -90f, scale * 360f);

        float inset = 3f * displayScaleFactor;
        clipRect.set(arcRect);
        clipRect.inset(inset, inset);

        canvas.save();
        {
            paint.setColor(trackColor);
            canvas.drawOval(arcRect, paint);

            paint.setColor(fillColor);
            canvas.drawPath(piePath, paint);

            paint.setColor(centerColor);
            canvas.drawOval(clipRect, paint);
        }
        canvas.restore();
    }


    public int getTrackColor() {
        return trackColor;
    }

    public void setTrackColor(int trackColor) {
        this.trackColor = trackColor;
        postInvalidate();
    }

    public int getCenterColor() {
        return centerColor;
    }

    public void setCenterColor(int centerColor) {
        this.centerColor = centerColor;
        postInvalidate();
    }

    @Override
    protected void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        super.initialize(attrs, defStyleAttr);

        this.displayScaleFactor = getResources().getDisplayMetrics().density;
        this.trackColor = getResources().getColor(R.color.timeline_border);
        this.centerColor = getResources().getColor(R.color.background);
    }
}
