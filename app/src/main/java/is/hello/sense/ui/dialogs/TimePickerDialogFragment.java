package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.widget.TimePicker;

import org.joda.time.LocalTime;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import is.hello.sense.ui.widget.RotaryTimePickerDialog;
import is.hello.sense.ui.widget.RotaryTimePickerView;

public class TimePickerDialogFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener, RotaryTimePickerDialog.OnTimeSetListener {
    public static final String TAG = TimePickerDialogFragment.class.getSimpleName();

    public static final int FLAG_USE_24_TIME = (1 << 1);
    public static final int FLAG_USE_ROTARY_PICKER = (1 << 2);

    @IntDef(value = {FLAG_USE_24_TIME, FLAG_USE_ROTARY_PICKER}, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Config {}

    private static final String ARG_DATE = TimePickerDialogFragment.class.getName() + ".ARG_DATE";
    private static final String ARG_CONFIG = TimePickerDialogFragment.class.getName() + ".ARG_CONFIG";
    private static final String ARG_AFFECTED_POSITION = TimePickerDialogFragment.class.getName() + ".ARG_AFFECTED_POSITION";

    public static final String RESULT_HOUR = TimePickerDialogFragment.class.getName() + ".RESULT_HOUR";
    public static final String RESULT_MINUTE = TimePickerDialogFragment.class.getName() + ".RESULT_MINUTE";
    public static final String RESULT_AFFECTED_POSITION = TimePickerDialogFragment.class.getName() + ".RESULT_AFFECTED_POSITION";

    private LocalTime time;
    private boolean use24Time;
    private boolean useRotaryPicker;

    public static TimePickerDialogFragment newInstance(@NonNull LocalTime date, @Config int config) {
        TimePickerDialogFragment dialog = new TimePickerDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date);
        arguments.putInt(ARG_CONFIG, config);
        dialog.setArguments(arguments);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.time = (LocalTime) getArguments().getSerializable(ARG_DATE);

        @Config int config = getArguments().getInt(ARG_CONFIG, 0);
        this.use24Time = ((config & FLAG_USE_24_TIME) == FLAG_USE_24_TIME);
        this.useRotaryPicker = ((Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) ||
                ((config & FLAG_USE_ROTARY_PICKER) == FLAG_USE_ROTARY_PICKER));
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (useRotaryPicker) {
            return new RotaryTimePickerDialog(getActivity(),
                                              this,
                                              time.getHourOfDay(),
                                              time.getMinuteOfHour(),
                                              use24Time);
        } else {
            return new TimePickerDialog(getActivity(),
                                        this,
                                        time.getHourOfDay(),
                                        time.getMinuteOfHour(),
                                        use24Time);
        }
    }

    public void setAffectedPosition(int position) {
        getArguments().putInt(ARG_AFFECTED_POSITION, position);
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
        onTimeSet(hour, minute);
    }

    @Override
    public void onTimeSet(RotaryTimePickerView view, int hour, int minute) {
        onTimeSet(hour, minute);
    }

    private void onTimeSet(int hour, int minute) {
        if (getTargetFragment() != null) {
            Intent response = new Intent();
            response.putExtra(RESULT_HOUR, hour);
            response.putExtra(RESULT_MINUTE, minute);
            response.putExtra(RESULT_AFFECTED_POSITION, getArguments().getInt(ARG_AFFECTED_POSITION, 0));
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
        }
    }
}
