package is.hello.sense.ui.handholding.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Constants;

public class DismissTutorialsDialog extends SenseDialogFragment {
    public static final String TAG = DismissTutorialsDialog.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        dialog.setTitle(R.string.title_dismiss_all);
        dialog.setMessage(R.string.message_dismiss_all);
        dialog.setPositiveButton(R.string.action_dismiss_all, (ignored, which) -> {
            SharedPreferences preferences = getActivity().getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
            preferences.edit()
                       .putBoolean(Constants.HANDHOLDING_SUPPRESSED, true)
                       .apply();

            if (getTargetFragment() != null) {
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            }
        });
        dialog.setNegativeButton(android.R.string.cancel, (ignored, which) -> {
            if (getTargetFragment() != null) {
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
            }
        });
        dialog.setDestructive(true);

        return dialog;
    }
}
