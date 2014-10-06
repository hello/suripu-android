package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import org.joda.time.DateTime;

public class DatePickerDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    public static final String TAG = DatePickerDialogFragment.class.getSimpleName();

    private static final String ARG_SEED_DATE = DatePickerDialogFragment.class.getName() + ".ARG_SEED_DATE";

    public static final String RESULT_YEAR = DatePickerDialogFragment.class.getName() + ".RESULT_YEAR";
    public static final String RESULT_MONTH = DatePickerDialogFragment.class.getName() + ".RESULT_MONTH";
    public static final String RESULT_DAY = DatePickerDialogFragment.class.getName() + ".RESULT_DAY";

    public static DatePickerDialogFragment newInstance(@Nullable DateTime seedDate) {
        DatePickerDialogFragment fragment = new DatePickerDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SEED_DATE, seedDate);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        DateTime seedTime = getSeedDate();
        return new DatePickerDialog(getActivity(),
                                    this,
                                    seedTime.getYear(),
                                    seedTime.getMonthOfYear() - 1,
                                    seedTime.getDayOfMonth());
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
        if (getTargetFragment() != null) {
            Intent response = new Intent();
            response.putExtra(RESULT_YEAR, year);
            response.putExtra(RESULT_MONTH, monthOfYear + 1);
            response.putExtra(RESULT_DAY, dayOfMonth);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
        }
    }


    private DateTime getSeedDate() {
        if (getArguments().containsKey(ARG_SEED_DATE)) {
            return (DateTime) getArguments().getSerializable(ARG_SEED_DATE);
        } else {
            return DateTime.now();
        }
    }
}
