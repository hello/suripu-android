package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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
    private final Path borderPath = new Path();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int intrinsicWidth;
    private final int intrinsicHeight;

    private int valueHeight;
    private @Nullable String value;
    private @Nullable Border border;


    //region Lifecycle

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

        borderPaint.setStyle(Paint.Style.STROKE);

        this.intrinsicWidth = resources.getDimensionPixelSize(R.dimen.view_grid_data_point_width);
        setMinimumWidth(intrinsicWidth);

        this.intrinsicHeight = resources.getDimensionPixelSize(R.dimen.view_grid_data_point_height);
        setMinimumHeight(intrinsicHeight);
    }

    //endregion


    //region Rendering

    @Override
    protected void onDraw(Canvas canvas) {
        final int midX = canvas.getWidth() / 2;
        final int midY = canvas.getHeight() / 2;

        fillRect.set(midX - (intrinsicWidth / 2),
                     midY - (intrinsicHeight / 2),
                     midX + (intrinsicWidth / 2),
                     midY + (intrinsicHeight / 2));
        canvas.drawOval(fillRect, fillPaint);

        if (!TextUtils.isEmpty(value)) {
            canvas.drawText(value,
                            fillRect.centerX(), fillRect.centerY() + (valueHeight / 2f),
                            textPaint);
        }

        if (border != null) {
            fillRect.inset(border.inset, border.inset);

            borderPath.reset();
            borderPath.addOval(fillRect, Path.Direction.CW);
            canvas.drawPath(borderPath, borderPaint);
        }
    }

    //endregion


    //region Attributes

    @Override
    protected boolean onSetAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
        borderPaint.setAlpha(alpha);
        return true;
    }

    public void setFillColor(@ColorInt int fillColor) {
        fillPaint.setColor(Drawing.colorWithAlpha(fillColor, fillPaint.getAlpha()));
        invalidate();
    }

    public void setValue(@Nullable String value) {
        this.value = value;
        if (TextUtils.isEmpty(value)) {
            this.valueHeight = 0;
        } else {
            final Rect textBounds = new Rect();
            textPaint.getTextBounds(value, 0, value.length(), textBounds);
            this.valueHeight = textBounds.height();
        }
        invalidate();
    }

    public void setBorder(@Nullable Border border) {
        if (border != null) {
            borderPaint.setColor(Drawing.colorWithAlpha(border.color, borderPaint.getAlpha()));
            borderPaint.setStrokeWidth(border.width);
        }

        this.border = border;
        invalidate();
    }

    //endregion


    public static class Border {
        public final @ColorInt int color;
        public final int inset;
        public final int width;

        public Border(int color, int inset, int width) {
            this.color = color;
            this.inset = inset;
            this.width = width;
        }
    }
}
