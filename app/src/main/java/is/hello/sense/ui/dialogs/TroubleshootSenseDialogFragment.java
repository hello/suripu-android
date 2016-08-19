package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.SenseAlertDialog;

public class TroubleshootSenseDialogFragment extends DialogFragment {
    public static final String TAG = TroubleshootSenseDialogFragment.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        dialog.setTitle(R.string.dialog_title_troubleshoot_sense);
        dialog.setMessage(R.string.dialog_message_troubleshoot_sense);

        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.setNegativeButton(R.string.action_help, (sender, which) -> {
            UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.PAIRING_SENSE_BLE);
        });

        return dialog;
    }
}
