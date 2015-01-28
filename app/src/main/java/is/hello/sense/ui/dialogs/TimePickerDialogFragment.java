package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.TimePicker;

import org.joda.time.LocalTime;

public class TimePickerDialogFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    public static final String TAG = TimePickerDialogFragment.class.getSimpleName();

    private static final String ARG_DATE = TimePickerDialogFragment.class.getName() + ".ARG_DATE";
    private static final String ARG_USE_24_TIME = TimePickerDialogFragment.class.getName() + ".ARG_USE_24_TIME";

    public static final String RESULT_HOUR = TimePickerDialogFragment.class.getName() + ".RESULT_HOUR";
    public static final String RESULT_MINUTE = TimePickerDialogFragment.class.getName() + ".RESULT_MINUTE";

    private LocalTime time;
    private boolean use24Time;

    public static TimePickerDialogFragment newInstance(@NonNull LocalTime date, boolean use24Time) {
        TimePickerDialogFragment dialog = new TimePickerDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date);
        arguments.putBoolean(ARG_USE_24_TIME, use24Time);
        dialog.setArguments(arguments);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.time = (LocalTime) getArguments().getSerializable(ARG_DATE);
        this.use24Time = getArguments().getBoolean(ARG_USE_24_TIME, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new TimePickerDialog(getActivity(), this, time.getHourOfDay(), time.getMinuteOfHour(), use24Time) {
            boolean ignoreEvent = false;

            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                // This only works in KitKat and below. Lollipop introduced
                // a new appearance for the picker that uses a radial clock,
                // and this new appearance only sends `onTimeChanged`
                // notifications after the user has finished editing all
                // of the fields in the picker. For Lollipop+, we just clamp
                // the time before sending it back to the invoker of this class.

                if (ignoreEvent) {
                    return;
                }

                if (shouldClampMinute(minute)) {
                    ignoreEvent = true;
                    view.setCurrentMinute(clampMinute(minute));
                    ignoreEvent = false;
                }
            }
        };
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
        if (getTargetFragment() != null) {
            Intent response = new Intent();
            response.putExtra(RESULT_HOUR, hour);
            response.putExtra(RESULT_MINUTE, clampMinute(minute));
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
        }
    }


    //region Clamping Times

    private static final int MINUTE_INTERVAL = 5;

    private static boolean shouldClampMinute(int minute) {
        return ((minute % MINUTE_INTERVAL) != 0);
    }

    private static int clampMinute(int minute) {
        if (shouldClampMinute(minute)) {
            // From <http://stackoverflow.com/questions/2580216/android-timepicker-minutes-to-15>
            int remainder = minute % MINUTE_INTERVAL;
            int minuteFloor = minute - remainder;
            int clampedMinute = minuteFloor + ((minute == minuteFloor + 1) ? MINUTE_INTERVAL : 0);
            if (clampedMinute >= 60) {
                clampedMinute = 0;
            }
            return clampedMinute;
        } else {
            return minute;
        }
    }

    //endregion
}
