package is.hello.sense.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class ChooseSoundDialogFragment extends DialogFragment {
    public static final String TAG = ChooseSoundDialogFragment.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // TODO: Get those sounds in here.

        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
}
