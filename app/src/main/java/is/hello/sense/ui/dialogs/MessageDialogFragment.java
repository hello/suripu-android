package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.ui.widget.SenseAlertDialog;

public final class MessageDialogFragment extends DialogFragment {
    public static final String TAG = MessageDialogFragment.class.getSimpleName();

    private static final String ARG_TITLE = MessageDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_MESSAGE = MessageDialogFragment.class.getName() + ".ARG_MESSAGE";

    public static MessageDialogFragment newInstance(@NonNull StringRef title,
                                                    @Nullable StringRef message) {
        MessageDialogFragment dialogFragment = new MessageDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_TITLE, title);
        arguments.putSerializable(ARG_MESSAGE, message);
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    public static MessageDialogFragment newInstance(@NonNull String title, @Nullable String message) {
        StringRef messageRef = message != null ? StringRef.from(message) : null;
        return newInstance(StringRef.from(title), messageRef);
    }

    public static MessageDialogFragment newInstance(@StringRes int titleRes, @StringRes int messageRes) {
        return newInstance(StringRef.from(titleRes), StringRef.from(messageRes));
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        StringRef title = (StringRef) getArguments().getSerializable(ARG_TITLE);
        dialog.setTitle(title.resolve(getActivity()));

        StringRef message = (StringRef) getArguments().getSerializable(ARG_MESSAGE);
        dialog.setMessage(message.resolve(getActivity()));

        dialog.setPositiveButton(android.R.string.ok, null);

        return dialog;
    }
}
