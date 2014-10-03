package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

@SuppressWarnings("UnusedDeclaration")
public class PieGraphView extends GraphView {
    private final Paint paint = new Paint();
    private final Path piePath = new Path();
    private final Path clipPath = new Path();
    private final RectF arcRect = new RectF();

    private float displayScaleFactor;

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
        super.onDraw(canvas);

        piePath.reset();
        clipPath.reset();

        int width = canvas.getWidth(), height = canvas.getHeight();
        float scale = ((float) value / (float) maxValue);
        piePath.moveTo(width / 2f, height / 2f);

        arcRect.set(0f, 0f, width, height);
        piePath.arcTo(arcRect, -90f, scale * 360f);

        float inset = 3f * displayScaleFactor;
        arcRect.inset(inset, inset);
        clipPath.addOval(arcRect, Path.Direction.CW);

        paint.setAntiAlias(true);
        paint.setColor(fillColor);

        canvas.save();
        {
            canvas.clipPath(clipPath, Region.Op.DIFFERENCE);
            canvas.drawPath(piePath, paint);
        }
        canvas.restore();
    }


    @Override
    protected void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        super.initialize(attrs, defStyleAttr);

        this.displayScaleFactor = getResources().getDisplayMetrics().density;
    }
}
