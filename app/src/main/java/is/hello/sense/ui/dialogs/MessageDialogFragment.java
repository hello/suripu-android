package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;

public final class MessageDialogFragment extends SenseDialogFragment {
    public static final String TAG = MessageDialogFragment.class.getSimpleName();

    private static final String ARG_TITLE = MessageDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_MESSAGE = MessageDialogFragment.class.getName() + ".ARG_MESSAGE";

    public static MessageDialogFragment newInstance(@NonNull StringRef title,
                                                    @Nullable StringRef message) {
        MessageDialogFragment dialogFragment = new MessageDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_TITLE, title);
        arguments.putParcelable(ARG_MESSAGE, message);
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
        Activity activity = getActivity();
        SenseAlertDialog dialog = new SenseAlertDialog(activity);

        StringRef title = getArguments().getParcelable(ARG_TITLE);
        if (title != null) {
            dialog.setTitle(title.resolve(activity));
        }

        StringRef message = getArguments().getParcelable(ARG_MESSAGE);
        if (message != null) {
            dialog.setMessage(message.resolve(activity));
        }

        dialog.setPositiveButton(android.R.string.ok, null);

        return dialog;
    }
}
