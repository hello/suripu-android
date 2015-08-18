package is.hello.sense.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import org.joda.time.LocalTime;

public class RotaryTimePickerDialog extends SenseAlertDialog implements RotaryTimePickerView.OnSelectionListener {
    private final RotaryTimePickerView rotaryTimePickerView;
    private final OnTimeSetListener onTimeSetListener;

    private boolean waitingForSelectionChange = false;
    private @Nullable Runnable afterSelectionChange;

    public RotaryTimePickerDialog(@NonNull Context context,
                                  @NonNull OnTimeSetListener onTimeSetListener,
                                  int hourOfDay,
                                  int minute,
                                  boolean is24HourView) {
        super(context);

        this.rotaryTimePickerView = new RotaryTimePickerView(context);
        rotaryTimePickerView.setUse24Time(is24HourView);
        rotaryTimePickerView.setTime(hourOfDay, minute);
        rotaryTimePickerView.setOnSelectionListener(this);
        setView(rotaryTimePickerView);

        this.onTimeSetListener = onTimeSetListener;

        setNegativeButton(android.R.string.cancel, null);
        setButtonDeemphasized(BUTTON_NEGATIVE, true);

        Button positiveButton = getButton(BUTTON_POSITIVE);
        positiveButton.setVisibility(View.VISIBLE);
        positiveButton.setText(android.R.string.ok);
        positiveButton.setOnClickListener(ignored -> {
            if (waitingForSelectionChange) {
                setButtonEnabled(BUTTON_POSITIVE, false);
                this.afterSelectionChange = this::onTimeSet;
            } else {
                onTimeSet();
            }
        });
        updateButtonDivider();
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        LocalTime selectedTime = (LocalTime) savedInstanceState.getSerializable("selectedTime");
        if (selectedTime != null) {
            rotaryTimePickerView.setTime(selectedTime);
        }

        Bundle parentSavedState = savedInstanceState.getParcelable("savedState");
        if (parentSavedState != null) {
            super.onRestoreInstanceState(parentSavedState);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle savedState = new Bundle();
        savedState.putSerializable("selectedTime", rotaryTimePickerView.getTime());
        savedState.putParcelable("savedState", super.onSaveInstanceState());
        return savedState;
    }

    public void updateTime(int hourOfDay, int minute) {
        rotaryTimePickerView.setTime(hourOfDay, minute);
    }

    private void onTimeSet() {
        onTimeSetListener.onTimeSet(rotaryTimePickerView.getHours(), rotaryTimePickerView.getMinutes());
        dismiss();
    }


    @Override
    public void onSelectionWillChange() {
        this.waitingForSelectionChange = true;
    }

    @Override
    public void onSelectionChanged() {
        this.waitingForSelectionChange = false;
        if (afterSelectionChange != null) {
            afterSelectionChange.run();
        }
    }


    public interface OnTimeSetListener {
        void onTimeSet(int hourOfDay, int minute);
    }
}
