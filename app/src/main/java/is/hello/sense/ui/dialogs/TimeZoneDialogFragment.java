package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.TimeZoneAdapter;

public class TimeZoneDialogFragment extends DialogFragment {
    public static final String TAG = TimeZoneDialogFragment.class.getSimpleName();
    public static final String RESULT_TIMEZONE_ID = TimeZoneDialogFragment.class.getName() + ".RESULT_TIMEZONE_ID";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.label_time_zone);
        TimeZoneAdapter adapter = new TimeZoneAdapter(getActivity());
        builder.setAdapter(adapter, (dialog, position) -> {
            if (getTargetFragment() != null) {
                Intent response = new Intent();
                response.putExtra(RESULT_TIMEZONE_ID, adapter.getItem(position));
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
}
