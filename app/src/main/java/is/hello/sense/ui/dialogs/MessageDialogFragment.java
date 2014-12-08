package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.ui.widget.SenseAlertDialog;

public final class MessageDialogFragment extends DialogFragment {
    public static final String TAG = MessageDialogFragment.class.getSimpleName();

    private static final String ARG_TITLE = MessageDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_MESSAGE = MessageDialogFragment.class.getName() + ".ARG_MESSAGE";

    public static MessageDialogFragment newInstance(@NonNull String title, @Nullable String message) {
        MessageDialogFragment dialogFragment = new MessageDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putString(ARG_MESSAGE, message);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        dialog.setTitle(getArguments().getString(ARG_TITLE));
        dialog.setMessage(getArguments().getString(ARG_MESSAGE));
        dialog.setPositiveButton(android.R.string.ok, null);

        return dialog;
    }
}
