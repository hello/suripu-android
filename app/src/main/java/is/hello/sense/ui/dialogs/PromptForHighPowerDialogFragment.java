package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;

public class PromptForHighPowerDialogFragment extends DialogFragment {
    public static final String TAG = PromptForHighPowerDialogFragment.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        dialog.setTitle(R.string.dialog_title_high_power_rescan);
        dialog.setMessage(R.string.dialog_message_high_power_rescan);

        dialog.setPositiveButton(R.string.action_retry, (sender, which) -> {
            Analytics.trackEvent(Analytics.Global.EVENT_TURN_ON_HIGH_POWER, null);
            sendResult(Activity.RESULT_OK);
        });
        dialog.setNegativeButton(android.R.string.cancel, (sender, which) -> {
            sendResult(Activity.RESULT_CANCELED);
        });

        return dialog;
    }

    private void sendResult(int result) {
        if (getTargetFragment() != null) {
            getTargetFragment().onActivityResult(getTargetRequestCode(), result, null);
        }
    }
}
