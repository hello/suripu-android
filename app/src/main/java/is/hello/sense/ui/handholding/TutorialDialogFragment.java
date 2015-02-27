package is.hello.sense.ui.handholding;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseDialogFragment;

public class TutorialDialogFragment extends SenseDialogFragment {
    private static final String ARG_TUTORIAL = TutorialDialogFragment.class.getName() + ".ARG_TUTORIAL";

    private Tutorial tutorial;

    public static TutorialDialogFragment newInstance(@NonNull Tutorial tutorial) {
        TutorialDialogFragment dialogFragment = new TutorialDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_TUTORIAL, tutorial);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.tutorial = (Tutorial) getArguments().getSerializable(ARG_TUTORIAL);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog overlay = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);

        return overlay;
    }
}
