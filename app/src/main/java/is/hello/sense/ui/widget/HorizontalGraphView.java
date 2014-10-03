package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

@SuppressWarnings("UnusedDeclaration")
public final class HorizontalGraphView extends GraphView {
    private final Paint paint = new Paint();

    public HorizontalGraphView(Context context) {
        super(context);
    }

    public HorizontalGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    //region Drawing

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = canvas.getWidth();
        float height = canvas.getHeight();
        float barWidth = ((float) value / (float) maxValue) * width;

        paint.setColor(fillColor);
        canvas.drawRect(0, 0, barWidth, height, paint);
    }

    //endregion
}
