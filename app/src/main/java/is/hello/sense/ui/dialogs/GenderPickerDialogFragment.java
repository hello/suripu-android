package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.api.model.Gender;

public class GenderPickerDialogFragment extends DialogFragment {
    public static final String TAG = GenderPickerDialogFragment.class.getSimpleName();

    public static final String RESULT_GENDER = GenderPickerDialogFragment.class.getName() + ".RESULT_GENDER";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.label_gender);
        builder.setNegativeButton(android.R.string.cancel, null);

        Gender.Adapter adapter = new Gender.Adapter(getActivity());
        builder.setAdapter(adapter, (unused, which) -> {
            if (getTargetFragment() != null) {
                Intent response = new Intent();
                response.putExtra(RESULT_GENDER, adapter.getItem(which).toString());
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
            }
        });

        return builder.create();
    }
}
