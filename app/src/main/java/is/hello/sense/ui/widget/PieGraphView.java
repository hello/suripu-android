package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import is.hello.sense.R;

@SuppressWarnings("UnusedDeclaration")
public final class PieGraphView extends GraphView {
    private final Path fillPath = new Path();
    private final RectF arcRect = new RectF();
    private Paint paint;

    private float fillStrokeWidth;
    private int trackColor;

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
        fillPath.reset();

        int width = canvas.getWidth(), height = canvas.getHeight();
        float scale = ((float) value / (float) maxValue);
        arcRect.set(0f, 0f, width, height);
        arcRect.inset(fillStrokeWidth / 2f, fillStrokeWidth / 2f);

        if (scale > 0f) {
            fillPath.moveTo(width / 2f, 0f);
            fillPath.arcTo(arcRect, -90f, scale * 360f);
        }

        canvas.save();
        {
            paint.setColor(trackColor);
            canvas.drawOval(arcRect, paint);

            paint.setColor(fillColor);
            canvas.drawPath(fillPath, paint);

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


    @Override
    protected void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        super.initialize(attrs, defStyleAttr);

        this.fillStrokeWidth = getResources().getDimensionPixelSize(R.dimen.pie_graph_stroke_width);
        this.trackColor = getResources().getColor(R.color.border);

        this.paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(fillStrokeWidth);
    }
}
