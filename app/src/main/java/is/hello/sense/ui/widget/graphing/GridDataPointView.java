package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Drawing;

public class GridDataPointView extends View {
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF fillRect = new RectF();

    private final int estimatedTextHeight;
    private final Point intrinsicSize;

    private @Nullable String value;


    //region Lifecycle

    public static Point getIntrinsicSize(@NonNull Resources resources) {
        return new Point(resources.getDimensionPixelSize(R.dimen.view_grid_data_point_width),
                         resources.getDimensionPixelSize(R.dimen.view_grid_data_point_height));
    }

    public GridDataPointView(@NonNull Context context) {
        this(context, null);
    }

    public GridDataPointView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridDataPointView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = getResources();

        Drawing.updateTextPaintFromStyle(textPaint, context, R.style.AppTheme_Text_GridDataPoint);
        textPaint.setTextAlign(Paint.Align.CENTER);

        this.estimatedTextHeight = Drawing.getEstimatedTextHeight(textPaint);
        this.intrinsicSize = getIntrinsicSize(resources);

        setMinimumWidth(intrinsicSize.x);
        setMinimumHeight(intrinsicSize.y);
    }

    //endregion


    //region Rendering

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();

        fillRect.set((width / 2) - (intrinsicSize.x / 2),
                     (height / 2) - (intrinsicSize.y / 2),
                     (width / 2) + (intrinsicSize.x / 2),
                     (height / 2) + (intrinsicSize.y / 2));
        canvas.drawOval(fillRect, fillPaint);

        if (!TextUtils.isEmpty(value)) {
            final float valueX = (width / 2f);
            final float valueY = height - estimatedTextHeight;
            canvas.drawText(value, valueX, valueY, textPaint);
        }
    }

    //endregion


    //region Attributes

    @Override
    protected boolean onSetAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
        return true;
    }

    public void setFillColor(@ColorInt int fillColor) {
        fillPaint.setColor(Drawing.colorWithAlpha(fillColor, fillPaint.getAlpha()));
        invalidate();
    }

    public void setValue(@Nullable String value) {
        this.value = value;
        invalidate();
    }

    //endregion
}
