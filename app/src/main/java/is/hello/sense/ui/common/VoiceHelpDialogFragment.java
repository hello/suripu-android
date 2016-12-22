package is.hello.sense.ui.common;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import rx.subjects.PublishSubject;

public class VoiceHelpDialogFragment extends SenseDialogFragment {

    public static final String TAG = VoiceHelpDialogFragment.class.getSimpleName() + "_TAG";

    /**
     * provide {@link PublishSubject} to observe dialog lifecycle changes.
     * Emits {@link Boolean} false when first shown and true when dismissed.
     */
    public final PublishSubject<Boolean> subject = PublishSubject.create();

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
            dismissSafely();
        });
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        dialog.setOnShowListener( listener -> subject.onNext(false));
        return dialog;
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        subject.onNext(true);
        subject.onCompleted();
        super.onDismiss(dialog);
    }
}
