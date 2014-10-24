package is.hello.sense.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.util.Analytics;

public class ErrorDialogFragment extends DialogFragment {
    public static final String TAG = ErrorDialogFragment.class.getSimpleName();

    private static final String ARG_MESSAGE = ErrorDialogFragment.class.getName() + ".ARG_MESSAGE";
    private static final String ARG_HAS_REQUEST_INFO = ErrorDialogFragment.class.getName() + ".ARG_HAS_REQUEST_INFO";
    private static final String ARG_URL = ErrorDialogFragment.class.getName() + ".ARG_URL";
    private static final String ARG_RESPONSE_STATUS = ErrorDialogFragment.class.getName() + ".ARG_RESPONSE_STATUS";
    private static final String ARG_RESPONSE_REASON = ErrorDialogFragment.class.getName() + ".ARG_RESPONSE_REASON";

    private static final int RESPONSE_STATUS_UNKNOWN = -1;

    public static void presentError(@NonNull FragmentManager fm, @Nullable Throwable e) {
        ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(e);
        fragment.show(fm, TAG);
    }

    public static ErrorDialogFragment newInstance(@Nullable Throwable e) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle arguments = new Bundle();
        if (e != null) {
            // RetrofitError is not properly Serializable! So we'll grab the things we're interested
            // out of the Throwable object and pull them out of the arguments on the other side.
            arguments.putString(ARG_MESSAGE, e.getMessage());

            if (e instanceof ApiException) {
                ApiException error = (ApiException) e;
                arguments.putBoolean(ARG_HAS_REQUEST_INFO, true);
                arguments.putString(ARG_URL, error.getUrl());
                if (error.getStatus() != null)
                    arguments.putInt(ARG_RESPONSE_STATUS, error.getStatus());
                arguments.putString(ARG_RESPONSE_REASON, error.getReason());
            }
        }
        fragment.setArguments(arguments);

        return fragment;
    }

    public static ErrorDialogFragment newInstance(@NonNull String message) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_MESSAGE, message);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.dialog_error_title);
        String message = getMessage();
        if (!TextUtils.isEmpty(message)) {
            if (hasRequestInfo()) {
                builder.setMessage(getString(R.string.dialog_error_extended_message_format, message, getResponseReason(), getResponseStatus()));
            } else {
                builder.setMessage(message);
            }
        } else {
            builder.setMessage(R.string.dialog_error_generic_message);
        }
        builder.setPositiveButton(android.R.string.ok, null);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        Analytics.error(getMessage(), getResponseStatus());
    }

    private String getMessage() {
        return getArguments().getString(ARG_MESSAGE);
    }

    private boolean hasRequestInfo() {
        return getArguments().getBoolean(ARG_HAS_REQUEST_INFO, false);
    }

    private String getUrl() {
        return getArguments().getString(ARG_URL);
    }

    private String getResponseReason() {
        return getArguments().getString(ARG_RESPONSE_REASON);
    }

    private int getResponseStatus() {
        return getArguments().getInt(ARG_RESPONSE_STATUS, RESPONSE_STATUS_UNKNOWN);
    }
}
