package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;

public class RotaryTimePickerDialog extends SenseAlertDialog {
    private final RotaryTimePickerView rotaryTimePickerView;
    private final OnTimeSetListener onTimeSetListener;

    public RotaryTimePickerDialog(@NonNull Context context,
                                  @NonNull OnTimeSetListener onTimeSetListener,
                                  int hourOfDay,
                                  int minute,
                                  boolean is24HourView) {
        super(context);

        this.rotaryTimePickerView = new RotaryTimePickerView(context);
        rotaryTimePickerView.setTime(hourOfDay, minute);
        rotaryTimePickerView.setUse24Time(is24HourView);
        setView(rotaryTimePickerView);

        this.onTimeSetListener = onTimeSetListener;

        setNegativeButton(android.R.string.cancel, null);
        setPositiveButton(android.R.string.ok, (dialog, which) -> onTimeSet());
    }




    public void updateTime(int hourOfDay, int minute) {
        rotaryTimePickerView.setTime(hourOfDay, minute);
    }

    private void onTimeSet() {
        onTimeSetListener.onTimeSet(rotaryTimePickerView, rotaryTimePickerView.getHours(), rotaryTimePickerView.getMinutes());
    }


    public interface OnTimeSetListener {
        void onTimeSet(RotaryTimePickerView view, int hourOfDay, int minute);
    }
}
