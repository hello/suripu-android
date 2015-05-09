package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.LocalTime;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import is.hello.sense.R;

public class RotaryTimePickerView extends LinearLayout implements RotaryPickerView.OnSelectionListener {
    private static final int PERIOD_AM = Calendar.AM;
    private static final int PERIOD_PM = Calendar.PM;

    //region Pickers

    private final RotaryPickerView hourPicker;
    private final TextView hourMinuteDivider;
    private final RotaryPickerView minutePicker;
    private final RotaryPickerView periodPicker;

    //endregion


    //region Attributes

    private boolean use24Time = false;
    private @Nullable OnSelectionListener onSelectionListener;

    //endregion


    //region Lifecycle

    public RotaryTimePickerView(@NonNull Context context) {
        this(context, null);
    }

    public RotaryTimePickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotaryTimePickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);

        getOverlay().add(new OverlayDrawable());

        LayoutParams pickerLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1);
        int hourMinutePadding = getResources().getDimensionPixelSize(R.dimen.gap_large);

        this.hourPicker = new RotaryPickerView(context);
        hourPicker.setOnSelectionListener(this);
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setWrapsAround(true);
        hourPicker.setWantsLeadingZeros(false);
        hourPicker.setItemGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        hourPicker.setItemHorizontalPadding(hourMinutePadding);
        addView(hourPicker, pickerLayoutParams);

        this.hourMinuteDivider = new TextView(context);
        hourMinuteDivider.setTextAppearance(context, RotaryPickerView.DEFAULT_ITEM_TEXT_APPEARANCE);
        hourMinuteDivider.setText(":");
        addView(hourMinuteDivider, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        this.minutePicker = new RotaryPickerView(context);
        minutePicker.setOnSelectionListener(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setWrapsAround(true);
        minutePicker.setItemGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        minutePicker.setItemHorizontalPadding(hourMinutePadding);
        addView(minutePicker, pickerLayoutParams);

        this.periodPicker = new RotaryPickerView(context);
        periodPicker.setOnSelectionListener(this);
        periodPicker.setMinValue(PERIOD_AM);
        periodPicker.setMaxValue(PERIOD_PM);
        periodPicker.setValueStrings(DateFormatSymbols.getInstance().getAmPmStrings());
        periodPicker.setItemGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        addView(periodPicker, pickerLayoutParams);

        if (attrs != null) {
            TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.RotaryPickerView, defStyleAttr, 0);

            int itemTextAppearance = styles.getResourceId(R.styleable.RotaryPickerView_senseTextAppearance, RotaryPickerView.DEFAULT_ITEM_TEXT_APPEARANCE);
            setItemTextAppearance(itemTextAppearance);

            Drawable itemBackground = styles.getDrawable(R.styleable.RotaryPickerView_senseItemBackground);
            setItemBackground(itemBackground);

            styles.recycle();
        }

        setTime(LocalTime.now());
    }

    //endregion


    //region Attributes

    public void setUse24Time(boolean use24Time) {
        if (use24Time == this.use24Time) {
            return;
        }

        if (use24Time) {
            periodPicker.setVisibility(GONE);

            hourPicker.setMaxValue(24);

            if (periodPicker.getValue() == PERIOD_PM) {
                hourPicker.setValue(hourPicker.getValue() + 12, false);
            }
        } else {
            periodPicker.setVisibility(VISIBLE);

            int hour = hourPicker.getValue();
            if (hour > 12) {
                periodPicker.setValue(PERIOD_PM, false);
                hourPicker.setValue(hour - 12, false);
            } else {
                periodPicker.setValue(PERIOD_AM, false);
            }

            hourPicker.setMaxValue(12);
        }

        this.use24Time = use24Time;
    }

    public void setTime(@NonNull LocalTime time) {
        int hour = time.getHourOfDay();
        int minute = time.getMinuteOfHour();

        if (use24Time) {
            hourPicker.setValue(hour, false);
        } else {
            if (hour > 12) {
                hourPicker.setValue(hour - 12, false);
                periodPicker.setValue(PERIOD_PM, false);
            } else {
                hourPicker.setValue(hour, false);
                periodPicker.setValue(PERIOD_AM, false);
            }
        }

        minutePicker.setValue(minute, false);
    }

    public LocalTime getTime() {
        int hour;
        if (use24Time) {
            hour = hourPicker.getValue();
        } else {
            hour = hourPicker.getValue();
            if (periodPicker.getValue() == PERIOD_PM) {
                hour += 12;
            }
        }
        int minute = minutePicker.getValue();
        return new LocalTime(hour, minute, 0);
    }

    public void setItemTextAppearance(@StyleRes int itemTextAppearance) {
        hourPicker.setItemTextAppearance(itemTextAppearance);
        minutePicker.setItemTextAppearance(itemTextAppearance);
        periodPicker.setItemTextAppearance(itemTextAppearance);
        hourMinuteDivider.setTextAppearance(getContext(), itemTextAppearance);
    }

    public void setItemBackground(@Nullable Drawable itemBackground) {
        hourPicker.setItemBackground(itemBackground);
        minutePicker.setItemBackground(itemBackground);
        periodPicker.setItemBackground(itemBackground);
    }

    public void setOnSelectionListener(@Nullable OnSelectionListener onSelectionListener) {
        this.onSelectionListener = onSelectionListener;
    }

    //endregion


    //region Callbacks


    @Override
    public void onSelectionWillChange(@NonNull RotaryPickerView pickerView) {
        if (onSelectionListener != null) {
            onSelectionListener.onSelectionWillChange(this);
        }
    }

    @Override
    public void onSelectionChanged(@NonNull RotaryPickerView pickerView, int newValue) {
        if (onSelectionListener != null) {
            onSelectionListener.onSelectionChanged(this);
        }
    }

    //endregion


    private class OverlayDrawable extends Drawable {
        private final Paint fillPaint = new Paint();
        private final int itemHeightHalf;

        private OverlayDrawable() {
            Resources resources = getResources();

            int color = resources.getColor(R.color.light_accent);
            fillPaint.setColor(color);
            fillPaint.setAlpha(32);

            this.itemHeightHalf = resources.getDimensionPixelSize(R.dimen.view_rotary_picker_height) / 2;
        }

        @Override
        public void draw(Canvas canvas) {
            int width = canvas.getWidth(),
                midY = canvas.getHeight() / 2;

            int top = midY - itemHeightHalf;
            int bottom = midY + itemHeightHalf;

            canvas.drawRect(0, top, width, bottom, fillPaint);
            canvas.drawRect(0, top, width, top + 1, fillPaint);
            canvas.drawRect(0, bottom - 1, width, bottom, fillPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            fillPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            fillPaint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }


    public interface OnSelectionListener {
        void onSelectionWillChange(@NonNull RotaryTimePickerView timePickerView);
        void onSelectionChanged(@NonNull RotaryTimePickerView timePickerView);
    }
}
