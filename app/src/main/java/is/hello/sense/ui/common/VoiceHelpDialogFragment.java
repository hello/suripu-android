package is.hello.sense.ui.common;

import android.app.Dialog;
import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;

public class VoiceHelpDialogFragment extends SenseDialogFragment {

    public static final String TAG = VoiceHelpDialogFragment.class.getSimpleName() + "_TAG";

    public static VoiceHelpDialogFragment newInstance(){
        return new VoiceHelpDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SenseAlertDialog dialog = SenseAlertDialog.newBottomSheetInstance(getActivity());
        final CharSequence clickableLink = getResources().getText(R.string.info_voice_tip);
        dialog.setMessage(Styles.resolveSupportLinks(getActivity(),
                                                     clickableLink));
        dialog.setTitle(R.string.title_voice_tip);
        dialog.setNegativeButton(R.string.action_close, (dialogInterface, which) -> {
            dismiss();
        });
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        return dialog;
    }
}
