package is.hello.sense.ui.widget.graphing;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Drawing;

public class GridGraphCellView extends View {
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF fillRect = new RectF();
    private final Path borderPath = new Path();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int intrinsicWidth;
    private final int intrinsicHeight;

    private int valueHeight;
    private @Nullable String value;
    private float borderInset;
    private @Nullable Border border;


    //region Lifecycle

    public GridGraphCellView(@NonNull Context context) {
        this(context, null);
    }

    public GridGraphCellView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridGraphCellView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = getResources();

        Drawing.updateTextPaintFromStyle(textPaint, context, R.style.AppTheme_Text_GridGraphCell);
        textPaint.setTextAlign(Paint.Align.CENTER);

        borderPaint.setStyle(Paint.Style.STROKE);

        this.intrinsicWidth = resources.getDimensionPixelSize(R.dimen.view_grid_graph_cell_width);
        setMinimumWidth(intrinsicWidth);

        this.intrinsicHeight = resources.getDimensionPixelSize(R.dimen.view_grid_graph_cell_height);
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
            fillRect.inset(borderInset, borderInset);

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
            final Resources resources = getResources();

            if (border.color != 0) {
                final @ColorInt int borderColor = resources.getColor(border.color);
                borderPaint.setColor(Drawing.colorWithAlpha(borderColor, borderPaint.getAlpha()));
            } else {
                borderPaint.setColor(Color.TRANSPARENT);
            }

            if (border.width != 0) {
                final float borderWidth = resources.getDimension(border.width);
                borderPaint.setStrokeWidth(borderWidth);
            } else {
                borderPaint.setStrokeWidth(0f);
            }

            if (border.inset != 0) {
                this.borderInset = resources.getDimension(border.inset);
            } else {
                this.borderInset = 0f;
            }
        }

        this.border = border;
        invalidate();
    }

    //endregion


    public static class Border {
        public final @ColorRes int color;
        public final @DimenRes int inset;
        public final @DimenRes int width;

        public Border(@ColorRes int color,
                      @DimenRes int inset,
                      @DimenRes int width) {
            this.color = color;
            this.inset = inset;
            this.width = width;
        }
    }

    public static final Border BORDER_OUTSIDE = new Border(R.color.border,
                                                           R.dimen.divider_size,
                                                           0);
    public static final Border BORDER_INSIDE = new Border(R.color.white,
                                                          R.dimen.divider_size,
                                                          R.dimen.divider_size);
}
