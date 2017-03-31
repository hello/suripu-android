package is.hello.sense.flows.voice.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.util.Logger;

public class VolumePickerView extends LinearLayout {
    //region Constants

    private static final String BUNDLE_MIN_VALUE = VolumePickerView.class.getName() + "MIN_VALUE";
    private static final String BUNDLE_MAX_VALUE = VolumePickerView.class.getName() + "MAX_VALUE";
    private static final String BUNDLE_SELECTED_VALUE = VolumePickerView.class.getName() + "SELECTED_VALUE";
    private static final String BUNDLE_SAVED_STATE = VolumePickerView.class.getName() + "SAVED_STATE";
    private static final int DEFAULT_MIN = 1;
    private static final int DEFAULT_MAX = 11;
    private static final int DEFAULT_INITIAL = 8;
    private static final float MIN_SCALE_FACTOR = 0.4f;

    //endregion

    private final int minSegmentSize;

    //region Properties
    private int minValue = DEFAULT_MIN;
    private int maxValue = DEFAULT_MAX;
    private int selectedValue = DEFAULT_INITIAL;
    private @Nullable OnValueChangedListener onValueChangedListener;
    private final List<Tick> ticks = new ArrayList<>(maxValue);
    private int activePointerId = -1;

    private final int normalTickColor;
    private final int emphasizedTickColor;
    private final int defaultTickSize;
    //endregion


    //region Lifecycle

    public VolumePickerView(final Context context) {
        this(context, null);
    }

    public VolumePickerView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VolumePickerView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);

        int initialValue = 0;
        this.minSegmentSize = context.getResources().getDimensionPixelSize(R.dimen.volume_picker_min_size);
        this.normalTickColor = ContextCompat.getColor(context, R.color.dim);
        this.emphasizedTickColor = ContextCompat.getColor(context, R.color.primary);
        this.defaultTickSize = getResources().getDimensionPixelSize(R.dimen.scale_view_tick);
        if (attrs != null) {
            initialValue = takeStateFromAttributes(attrs, defStyleAttr);
        }

        setValue(initialValue, false);
        addTicks();
    }

    private int takeStateFromAttributes(@NonNull final AttributeSet attrs, final int defStyleAttr) {
        final TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.VolumePickerView, defStyleAttr, 0);

        this.minValue = styles.getInteger(R.styleable.VolumePickerView_minValue, DEFAULT_MIN);
        this.maxValue = styles.getInteger(R.styleable.VolumePickerView_maxValue, DEFAULT_MAX);
        final int initialValue = styles.getInteger(R.styleable.VolumePickerView_initialValue, DEFAULT_INITIAL);

        styles.recycle();

        return initialValue;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                this.activePointerId = MotionEventCompat.getPointerId(event, 0);
                updateTicksOnTouch(MotionEventCompat.getX(event, 0));
                return true;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                return false; //don't allow multiple pointers
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                if(pointerId == this.activePointerId) {
                    updateTicksOnTouch(MotionEventCompat.getX(event, pointerIndex));
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    this.activePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                // add the missing redundant check
                final int pointerIndex = MotionEventCompat.findPointerIndex(event, activePointerId);
                if (pointerIndex < 0) {
                    Logger.warn(getClass().getSimpleName(), "Reached edge-case that causes out of bounds pointer id exception");
                    return false;
                }

                break;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;

            setMinValue(savedState.getInt(BUNDLE_MIN_VALUE, DEFAULT_MIN));
            setMaxValue(savedState.getInt(BUNDLE_MAX_VALUE, DEFAULT_MAX));
            setValue(savedState.getInt(BUNDLE_SELECTED_VALUE, DEFAULT_INITIAL), true);

            state = savedState.getParcelable(BUNDLE_SAVED_STATE);
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle savedState = new Bundle();
        savedState.putParcelable(BUNDLE_SAVED_STATE, super.onSaveInstanceState());
        savedState.putInt(BUNDLE_MIN_VALUE, minValue);
        savedState.putInt(BUNDLE_MAX_VALUE, maxValue);
        savedState.putInt(BUNDLE_SELECTED_VALUE, selectedValue);
        return savedState;
    }


    //endregion

    public void onDestroyView() {
        onValueChangedListener = null;


        ticks.clear();
        removeAllViews();
    }

    //region Internal

    protected void addTicks() {
        final int itemCount = getItemCount();
        post( () -> {
            final int parentWidth = getMeasuredWidth();
            final int parentHeight = getMeasuredHeight();

            for (int i = 0; i < itemCount; i++) {
                final float scale = VolumePickerView.getScaleFactorFromPosition(i, itemCount);
                final Tick tick = newTickInstance(scale);
                tick.setMinimumWidth(minSegmentSize);
                tick.setEmphasized(shouldEmphasizeTick(i));
                tick.setLayoutParams(new LinearLayout.LayoutParams(parentWidth / itemCount,
                                                                   parentHeight,
                                                                   Gravity.CENTER_HORIZONTAL));
                ticks.add(tick);
                VolumePickerView.this.addView(tick, i);
            }

            post(this::notifyValueChangedListener);
        });
    }

    private Tick newTickInstance(final float scale) {
        return new Tick(getContext(),
                        defaultTickSize,
                        normalTickColor,
                        emphasizedTickColor,
                        scale);
    }

    protected void updateTicksOnTouch(final float touchedXPos){
        if(!isTouchInBounds(touchedXPos)){
            return;
        }
        for (int i = 0; i < ticks.size(); i++) {
            final Tick t = ticks.get(i);
            if(touchedXPos >= t.getLeft()){
                if(touchedXPos <= t.getRight()){
                    selectedValue = getValueFromPosition(i);
                }
                t.setEmphasized(true);
            } else {
                t.setEmphasized(false);
            }
        }
        post(this::notifyValueChangedListener);
    }

    private boolean isTouchInBounds(final float touchedXPos) {
        return !ticks.isEmpty() && touchedXPos >= ticks.get(0).getLeft() //first tick
                && touchedXPos <= ticks.get(ticks.size() - 1).getRight(); //last tick
    }

    protected void updateTicksOnSelectedValue(){
        for (int i = 0; i < ticks.size(); i++) {
            final Tick tick = ticks.get(i);
            tick.setEmphasized(shouldEmphasizeTick(i));
        }
    }

    protected boolean shouldEmphasizeTick(final int position) {
        return selectedValue >= getValueFromPosition(position);
    }

    protected static float getScaleFactorFromPosition(final int position, final int itemCount){
        return MIN_SCALE_FACTOR + (1 - MIN_SCALE_FACTOR) * (((float) position) / itemCount);
    }

    protected int getValueFromPosition(final int position){
        return minValue + position;
    }

    protected void notifyValueChangedListener() {
        if (onValueChangedListener != null) {
            onValueChangedListener.onValueChanged(selectedValue);
        }
    }

    //endregion


    //region Properties

    //todo need to be able to update ticks if dynamically change min max values
    public void setMinValue(final int minValue) {
        this.minValue = minValue;
    }

    public void setMaxValue(final int maxValue) {
        this.maxValue = maxValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue(){
        return maxValue;
    }

    public void setValue(final int newValue, final boolean notifyListener) {
        this.selectedValue = newValue;
        if (notifyListener) {
            post(this::notifyValueChangedListener);
        }
        post(this::updateTicksOnSelectedValue);
    }

    public int getValue() {
        return selectedValue;
    }

    public int getItemCount(){
        return maxValue - minValue + 1;
    }

    public void setOnValueChangedListener(@Nullable final OnValueChangedListener onValueChangedListener) {
        this.onValueChangedListener = onValueChangedListener;
    }

    //endregion

    private static class Tick extends View {
        private final Paint linePaint = new Paint();
        @Dimension private final int lineSize;
        @ColorInt private final int normalColor;
        @ColorInt private final int emphasizedColor;

        private final float extentScale;

        private Tick(@NonNull final Context context,
                     @Dimension final int lineSize,
                     @ColorInt final int normalColor,
                     @ColorInt final int emphasizedColor,
                     final float scale){
            super(context);

            this.lineSize = lineSize;
            this.normalColor = normalColor;
            this.emphasizedColor = emphasizedColor;
            this.extentScale = scale;
            setSaveEnabled(false);
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
