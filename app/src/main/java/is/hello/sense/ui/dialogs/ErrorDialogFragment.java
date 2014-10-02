package is.hello.sense.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;

import is.hello.sense.R;

public class ErrorDialogFragment extends DialogFragment {
    public static final String TAG = ErrorDialogFragment.class.getSimpleName();
    private static final String ARG_THROWABLE = ErrorDialogFragment.class.getName() + ".ARG_THROWABLE";

    private Throwable throwable;

    public static void presentError(@NonNull FragmentManager fm, @Nullable Throwable e) {
        ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(e);
        fragment.show(fm, TAG);
    }

    public static ErrorDialogFragment newInstance(@Nullable Throwable e) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_THROWABLE, e);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.throwable = (Throwable) getArguments().getSerializable(ARG_THROWABLE);

        setCancelable(true);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.dialog_error_title);
        if (throwable != null && !TextUtils.isEmpty(throwable.getMessage())) {
            builder.setMessage(throwable.getMessage());
        } else {
            builder.setMessage(R.string.dialog_error_generic_message);
        }
        builder.setPositiveButton(android.R.string.ok, null);

        return builder.create();
    }
}
