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

    private final int minSegmentSize;

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
        this.minSegmentSize = context.getResources().getDimensionPixelSize(R.dimen.volume_picker_min_size);
        if (attrs != null) {
            initialValue = takeStateFromAttributes(attrs, defStyleAttr);
        }

        setValue(initialValue, false);
        addTicks();
        setOnTouchListener((ignored, event) -> {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    updateTicksOnTouch(event.getX());
                    break;
            }
            return false;
        });
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
        final Bundle savedState = new Bundle();
        savedState.putParcelable("savedState", super.onSaveInstanceState());
        savedState.putInt("minValue", minValue);
        savedState.putInt("maxValue", maxValue);
        savedState.putInt("value", selectedValue);
        return savedState;
    }


    //endregion

    public void onDestroyView() {
        if(onValueChangedListener != null){
            onValueChangedListener = null;
        }

        setOnTouchListener(null);
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
                final Tick tick = new Tick(getContext(), scale);
                tick.setMinimumWidth(minSegmentSize);
                tick.setEmphasized(shouldEmphasizeTick(i));
                tick.setLayoutParams(new LinearLayout.LayoutParams(parentWidth / itemCount,
                                                                   parentHeight,
                                                                   Gravity.CENTER_HORIZONTAL));
                ticks.add(tick);
                VolumePickerView.this.addView(tick, i);
            }

            invalidate();
            post(this::notifyValueChangedListener);
        });
    }

    protected void updateTicksOnTouch(final float touchedXPos){
        float t_x;
        for (int i = 0; i < ticks.size(); i++) {
            final Tick t = ticks.get(i);
            t_x = t.getX();
            if(touchedXPos >= t_x){
                if(touchedXPos < t_x + t.getWidth()){
                    selectedValue = getValueFromPosition(i);
                }
                t.setEmphasized(true);
            } else {
                t.setEmphasized(false);
            }
        }
        post(this::notifyValueChangedListener);
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

    /**
     * @param pValue must be between 0 - 100 %
     * @return converted value between {@link this#minValue} and {@link this#maxValue}
     */
    public int convertFromPercentageValue(final int pValue){
        return (int) Math.floor((pValue / 100f) * getItemCount());
    }

    /**
     * @return converted value between 0 and 100
     */
    public int convertSelectedValueToPercentageValue(){
        return (int) Math.ceil((selectedValue * 100f) / getItemCount());
    }

    //endregion

    private static class Tick extends View {
        private final Paint linePaint = new Paint();
        private final int lineSize;
        private final int normalColor;
        private final int emphasizedColor;

        private final float extentScale;

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
