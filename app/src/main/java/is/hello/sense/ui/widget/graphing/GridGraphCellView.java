package is.hello.sense.ui.widget.graphing;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.lang.ref.WeakReference;

import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.widget.util.Drawing;

public class GridGraphCellView extends View {
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF fillRect = new RectF();
    private final Path borderPath = new Path();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int valueHeight;
    private int intrinsicWidth;
    private int intrinsicHeight;
    private boolean drawValue;

    private @Nullable String value;
    private float borderInset;
    private @NonNull Border border = Border.NONE;
    private Size size;

    private @Nullable WeakReference<ValueAnimator> fillColorAnimator;


    //region Lifecycle

    public GridGraphCellView(@NonNull Context context) {
        this(context, null);
    }

    public GridGraphCellView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridGraphCellView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Drawing.updateTextPaintFromStyle(textPaint, context, R.style.AppTheme_Text_GridGraphCell);
        textPaint.setTextAlign(Paint.Align.CENTER);

        this.valueHeight = Drawing.getEstimatedLineHeight(textPaint, true);

        borderPaint.setStyle(Paint.Style.STROKE);

        setSize(Size.REGULAR);
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

        if (drawValue && !TextUtils.isEmpty(value)) {
            canvas.drawText(value,
                            fillRect.centerX(), fillRect.centerY() + (valueHeight / 2f),
                            textPaint);
        }

        if (border != Border.NONE) {
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
        final ValueAnimator fillColorAnimator = Functions.extract(this.fillColorAnimator);
        if (fillColorAnimator != null) {
            fillColorAnimator.cancel();
        }

        fillPaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
        borderPaint.setAlpha(alpha);

        return true;
    }

    public void setFillColor(@ColorInt int fillColor) {
        final ValueAnimator fillColorAnimator = Functions.extract(this.fillColorAnimator);
        if (fillColorAnimator != null) {
            fillColorAnimator.cancel();
        }

        fillPaint.setColor(Drawing.colorWithAlpha(fillColor, fillPaint.getAlpha()));
        invalidate();
    }

    public void setValue(@Nullable String value) {
        this.value = value;
        invalidate();
    }

    public void setBorder(@NonNull Border border) {
        if (border.color != 0) {
            final @ColorInt int borderColor = ContextCompat.getColor(getContext(), border.color);
            borderPaint.setColor(Drawing.colorWithAlpha(borderColor, borderPaint.getAlpha()));
        } else {
            borderPaint.setColor(Color.TRANSPARENT);
        }

        final Resources resources = getResources();
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

        this.border = border;
        invalidate();
    }

    public void setSize(@NonNull Size size) {
        if (size != this.size) {
            this.size = size;

            final Resources resources = getResources();
            this.intrinsicWidth = size.getWidth(resources);
            setMinimumWidth(intrinsicWidth);

            this.intrinsicHeight = size.getHeight(resources);
            setMinimumHeight(intrinsicHeight);

            this.drawValue = size.drawValue;

            invalidate();
        }
    }

    @Nullable
    public ValueAnimator createFillColorAnimator(@ColorInt int newColor) {
        final @ColorInt int startColor = fillPaint.getColor();
        final @ColorInt int endColor = Drawing.colorWithAlpha(newColor, fillPaint.getAlpha());
        if (startColor == endColor) {
            return null;
        }

        final ValueAnimator fillColorAnimator =
                AnimatorTemplate.DEFAULT.createColorAnimator(startColor, endColor);
        fillColorAnimator.addUpdateListener(animator -> {
            final @ColorInt int color = (int) animator.getAnimatedValue();
            fillPaint.setColor(color);
            invalidate();
        });

        this.fillColorAnimator = new WeakReference<>(fillColorAnimator);

        return fillColorAnimator;
    }

    //endregion


    public enum Size {
        REGULAR(R.dimen.view_grid_graph_cell_width,
                R.dimen.view_grid_graph_cell_height,
                true),
        SMALL(R.dimen.view_grid_graph_cell_width_small,
              R.dimen.view_grid_graph_cell_height_small,
              false);

        final @DimenRes int width;
        final @DimenRes int height;
        final boolean drawValue;

        public int getWidth(@NonNull Resources resources) {
            return resources.getDimensionPixelSize(width);
        }

        public int getHeight(@NonNull Resources resources) {
            return resources.getDimensionPixelSize(height);
        }

        Size(@DimenRes int width,
             @DimenRes int height,
             boolean drawValue) {
            this.width = width;
            this.height = height;
            this.drawValue = drawValue;
        }
    }

    public enum Border {
        NONE(0, 0, 0),
        OUTSIDE(R.color.border,
                R.dimen.divider_size,
                0),
        INSIDE(R.color.white,
               R.dimen.divider_size_thick,
               R.dimen.divider_size_thick);

        public final @ColorRes int color;
        public final @DimenRes int inset;
        public final @DimenRes int width;

        Border(@ColorRes int color,
               @DimenRes int inset,
               @DimenRes int width) {
            this.color = color;
            this.inset = inset;
            this.width = width;
        }
    }
}
