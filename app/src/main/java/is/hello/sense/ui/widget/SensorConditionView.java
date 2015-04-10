package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;

public class SensorConditionView extends View {
    private @Nullable Drawable icon;

    //region Lifecycle

    public SensorConditionView(@NonNull Context context) {
        this(context, null);
    }

    public SensorConditionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SensorConditionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (getBackground() == null) {
            setBackgroundResource(R.drawable.room_check_sensor_border_empty);
        }

        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SensorConditionView, defStyleAttr, 0);
            this.icon = attributes.getDrawable(R.styleable.SensorConditionView_senseSensorIcon);
            attributes.recycle();
        }
    }

    //endregion


    //region Rendering

    @Override
    protected void onDraw(Canvas canvas) {
        if (icon != null) {
            int canvasMidX = canvas.getWidth() / 2;
            int canvasMidY = canvas.getHeight() / 2;

            int iconMidX = icon.getIntrinsicWidth() / 2;
            int iconMidY = icon.getIntrinsicHeight() / 2;

            icon.setBounds(canvasMidX - iconMidX, canvasMidY - iconMidY,
                           canvasMidX + iconMidX, canvasMidY + iconMidY);
            icon.draw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable background = getBackground();

        int intrinsicWidth = 0,
            intrinsicHeight = 0;
        if (background != null) {
            intrinsicWidth = background.getIntrinsicWidth();
            intrinsicHeight = background.getIntrinsicHeight();
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec),
            heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec),
            height = MeasureSpec.getSize(heightMeasureSpec);

        int measuredWidth,
            measuredHeight;
        if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = Math.min(intrinsicWidth, width);
        } else {
            measuredWidth = width;
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = Math.min(intrinsicHeight, height);
        } else {
            measuredHeight = height;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    //endregion


    //region Display

    private void setIcon(@DrawableRes int iconRes) {
        if (this.icon != null) {
            this.icon.setCallback(null);
        }

        this.icon = getResources().getDrawable(iconRes).mutate();

        if (this.icon != null) {
            this.icon.setCallback(this);
        }

        invalidate();
    }

    private void applyTint(@ColorRes int colorRes) {
        int color = getResources().getColor(colorRes);
        if (icon != null) {
            icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }

        Drawable background = getBackground();
        if (getBackground() != null) {
            Drawable tintedBackground = background.mutate();
            tintedBackground.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            setBackground(tintedBackground);
        }
    }

    public void showInactiveIcon(@DrawableRes int iconRes) {
        setBackgroundResource(R.drawable.room_check_sensor_border_empty);
        setIcon(iconRes);
    }

    public void showInProgressIcon(@DrawableRes int iconRes) {
        setBackgroundResource(R.drawable.room_check_progress_drawable);
        setIcon(iconRes);
        applyTint(R.color.light_accent);
    }

    public void showCompletedIcon(@DrawableRes int iconRes, @NonNull Condition condition) {
        setBackgroundResource(R.drawable.room_check_sensor_border_filled);
        setIcon(iconRes);
        applyTint(condition.colorRes);
    }

    //endregion
}
