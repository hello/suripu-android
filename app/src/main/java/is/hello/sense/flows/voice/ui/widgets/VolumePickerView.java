package is.hello.sense.flows.voice.ui.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;

public class VolumePickerView extends LinearLayout {
    //region Constants

    private static final int DEFAULT_MIN = 1;
    private static final int DEFAULT_MAX = 11;
    private static final int DEFAULT_INITIAL = 8;
    private static final float MIN_SCALE_FACTOR = 0.4f;

    //endregion

    private final int segmentSize;

    private boolean animating = false;

    //region Properties
    private int minValue = DEFAULT_MIN;
    private int maxValue = DEFAULT_MAX;
    private int selectedValue = DEFAULT_INITIAL;
    private @Nullable OnValueChangedListener onValueChangedListener;
    private final List<Tick> ticks = new ArrayList<>(maxValue);

    //endregion


    //region Lifecycle

    public VolumePickerView(Context context) {
        this(context, null);
    }

    public VolumePickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VolumePickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);

        int initialValue = 0;
        this.segmentSize = context.getResources().getDimensionPixelSize(R.dimen.volume_picker_min_size);
        if (attrs != null) {
            initialValue = takeStateFromAttributes(attrs, defStyleAttr);
        }

        setValue(initialValue, false);
        addTicks();
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        final float x = event.getX();
                        float t_x;
                        for (int i = 0; i < ticks.size(); i++) {
                            final Tick t = ticks.get(i);
                            t_x = t.getX();
                            if(x >= t_x){
                                if(x < t_x + t.getWidth()){
                                   setValue(minValue + i, false);
                                }
                                t.setEmphasized(true);
                            } else {
                                t.setEmphasized(false);
                            }
                        }
                        notifyValueChangedListener();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        //todo handle drags
                }
                return false;
            }
        });
    }



    private int takeStateFromAttributes(@NonNull final AttributeSet attrs, final int defStyleAttr) {
        //todo make unique style attributes
        final TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.ScaleView, defStyleAttr, 0);

        this.minValue = styles.getInteger(R.styleable.ScaleView_scaleMinValue, DEFAULT_MIN);
        this.maxValue = styles.getInteger(R.styleable.ScaleView_scaleMaxValue, DEFAULT_MAX);
        final int initialValue = styles.getInteger(R.styleable.ScaleView_scaleValue, DEFAULT_INITIAL);

        styles.recycle();

        return initialValue;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;

            setMinValue(savedState.getInt("minValue"));
            setMaxValue(savedState.getInt("maxValue"));
            setValue(savedState.getInt("value"), true);

            state = savedState.getParcelable("savedState");
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle savedState = new Bundle();
        savedState.putParcelable("savedState", super.onSaveInstanceState());
        savedState.putInt("minValue", minValue);
        savedState.putInt("maxValue", maxValue);
        savedState.putInt("value", selectedValue);
        return savedState;
    }


    //endregion


    //region Drawing

    public void onDestroyView() {
        if(onValueChangedListener != null){
            onValueChangedListener = null;
        }
        //doesn't seem to prevent child views being restored
        removeAllViews();
    }

    //endregion


    //region Internal

    protected void addTicks() {
        final int itemCount = maxValue - minValue;
        post( () -> {
            final int parentWidth = getMeasuredWidth();
            final int parentHeight = getMeasuredHeight();
            //todo need to fix restore state
            if(getChildCount() > itemCount){
                return;
            }
            for (int i = 0; i <= itemCount; i++) {
                final float scale = MIN_SCALE_FACTOR + (1 - MIN_SCALE_FACTOR) * (((float) i) / itemCount);
                final Tick tick = new Tick(getContext(), scale);
                tick.setMinimumWidth(segmentSize);
                tick.setEmphasized(i + minValue <= selectedValue);
                tick.setLayoutParams(new LinearLayout.LayoutParams(parentWidth / itemCount,
                                                                   parentHeight,
                                                                   Gravity.CENTER_HORIZONTAL));
                ticks.add(tick);
                VolumePickerView.this.addView(tick);
            }

        invalidate();

        });
    }

    protected void notifyValueChangedListener() {
        if (onValueChangedListener != null) {
            onValueChangedListener.onValueChanged(getValue());
        }
    }

    //endregion


    //region Properties

    public void setMinValue(final int minValue) {
        this.minValue = minValue;
        addTicks();
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMaxValue(final int maxValue) {
        this.maxValue = maxValue;
        addTicks();
    }

    public void setValue(final int newValue, final boolean notifyListener) {
        this.selectedValue = newValue;
        final int newPosition = (newValue - minValue - 1);
        if (notifyListener) {
            post(this::notifyValueChangedListener);
        }
    }

    public int getValue() {
        return selectedValue;
    }

    public boolean isAnimating() {
        return animating;
    }

    public void setOnValueChangedListener(@Nullable final OnValueChangedListener onValueChangedListener) {
        this.onValueChangedListener = onValueChangedListener;
    }

    //endregion

    private class Tick extends View {
        private final Paint linePaint = new Paint();
        private final int lineSize;
        private final int normalColor;
        private final int emphasizedColor;

        private float extentScale;

        private Tick(@NonNull final Context context, final float scale) {
            this(context,
                 R.dimen.scale_view_tick,
                 R.color.voice_volume_normal_color,
                 R.color.voice_volume_emphasized_color,
                 scale);
        }

        private Tick(@NonNull final Context context,
                     @DimenRes final int lineSizeRes,
                     @ColorRes final int normalColorRes,
                     @ColorRes final int emphasizedColorRes,
                     final float scale){
            super(context);

            final Resources resources = context.getResources();
            this.lineSize = resources.getDimensionPixelSize(lineSizeRes);
            this.normalColor = ContextCompat.getColor(context, normalColorRes);
            this.emphasizedColor = ContextCompat.getColor(context, emphasizedColorRes);
            this.extentScale = scale;
            setSaveEnabled(false);
        }

        public void setExtentScale(final float extentScale){
            this.extentScale = extentScale;
            invalidate();
        }

        protected void setEmphasized(final boolean emphasized) {
            if (emphasized) {
                linePaint.setColor(emphasizedColor);
            } else {
                linePaint.setColor(normalColor);
            }
            invalidate();
        }

        @Override
        protected void onDraw(final Canvas canvas) {
            final float maxY = canvas.getHeight();

                final float minY = maxY * (1 - extentScale);
                canvas.drawRect(0, minY, lineSize, maxY, linePaint);
        }
    }

    public interface OnValueChangedListener {
        void onValueChanged(int newValue);
    }
}
