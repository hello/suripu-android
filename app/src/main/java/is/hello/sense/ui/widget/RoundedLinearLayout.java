package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import is.hello.sense.R;

public class RoundedLinearLayout extends LinearLayout {
    //region Fields

    private final Path clippingPath = new Path();
    private final RectF clippingRect = new RectF();

    private final float[] cornerRadii = new float[8];

    //endregion


    //region Lifecycle

    public RoundedLinearLayout(@NonNull Context context) {
        this(context, null);
    }

    public RoundedLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.RoundedLayout, defStyleAttr, 0);

            float radius = values.getDimension(R.styleable.RoundedLayout_senseCornerRadius, 0f);
            setCornerRadii(radius);

            values.recycle();
        }
    }

    //endregion


    //region Drawing

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        canvas.save();
        canvas.clipPath(clippingPath);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        clippingPath.reset();
        clippingRect.set(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
        clippingPath.addRoundRect(clippingRect, cornerRadii, Path.Direction.CW);
    }

    //endregion


    //region Properties

    public void setCornerRadii(float topLeft, float topRight, float bottomLeft, float bottomRight) {
        cornerRadii[0] = topLeft;
        cornerRadii[1] = topLeft;

        cornerRadii[2] = topRight;
        cornerRadii[3] = topRight;

        cornerRadii[4] = bottomLeft;
        cornerRadii[5] = bottomLeft;

        cornerRadii[6] = bottomRight;
        cornerRadii[7] = bottomRight;

        invalidate();
    }

    public void setCornerRadii(float value) {
        setCornerRadii(value, value, value, value);
    }

    //endregion
}
