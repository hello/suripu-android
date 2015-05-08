package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import org.joda.time.LocalTime;

import is.hello.sense.R;

public class RotaryTimePickerView extends LinearLayout implements RotaryPickerView.OnSelectionListener {
    private static final int PERIOD_AM = 0;
    private static final int PERIOD_PM = 1;

    //region Pickers

    private final RotaryPickerView hourPicker;
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

        LayoutParams pickerLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        this.hourPicker = new RotaryPickerView(context);
        hourPicker.setOnSelectionListener(this);
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        addView(hourPicker, pickerLayoutParams);

        LayoutParams minutePickerLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        int margin = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        minutePickerLayoutParams.setMargins(margin, 0, margin, 0);
        this.minutePicker = new RotaryPickerView(context);
        minutePicker.setOnSelectionListener(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setWrapsAround(true);
        addView(minutePicker, minutePickerLayoutParams);

        this.periodPicker = new RotaryPickerView(context);
        periodPicker.setOnSelectionListener(this);
        periodPicker.setMinValue(PERIOD_AM);
        periodPicker.setMaxValue(PERIOD_PM);
        periodPicker.setValueStrings(new String[]{"am", "pm"});
        addView(periodPicker, pickerLayoutParams);

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
                hourPicker.setValue(hourPicker.getValue() + 12);
            }
        } else {
            periodPicker.setVisibility(VISIBLE);

            int hour = hourPicker.getValue();
            if (hour > 12) {
                periodPicker.setValue(PERIOD_PM);
                hourPicker.setValue(hour - 12);
            } else {
                periodPicker.setValue(PERIOD_AM);
            }

            hourPicker.setMaxValue(12);
        }

        this.use24Time = use24Time;
    }

    public void setTime(@NonNull LocalTime time) {
        int hour = time.getHourOfDay();
        int minute = time.getMinuteOfHour();

        if (use24Time) {
            hourPicker.setValue(hour);
        } else {
            if (hour > 12) {
                hourPicker.setValue(hour - 12);
                periodPicker.setValue(PERIOD_PM);
            } else {
                hourPicker.setValue(hour);
                periodPicker.setValue(PERIOD_AM);
            }
        }

        minutePicker.setValue(minute);
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

    public void setOnSelectionListener(@Nullable OnSelectionListener onSelectionListener) {
        this.onSelectionListener = onSelectionListener;
    }

    //endregion


    //region Callbacks

    @Override
    public void onSelectionChanged(@NonNull RotaryPickerView pickerView, int newValue) {
        if (onSelectionListener != null) {
            onSelectionListener.onSelectionChanged(this);
        }
    }

    //endregion


    public interface OnSelectionListener {
        void onSelectionChanged(@NonNull RotaryTimePickerView timePickerView);
    }
}
