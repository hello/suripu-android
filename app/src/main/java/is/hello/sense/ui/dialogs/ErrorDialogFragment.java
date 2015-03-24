package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.ui.activities.SupportActivity;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Errors;

public class ErrorDialogFragment extends DialogFragment {
    public static final String TAG = ErrorDialogFragment.class.getSimpleName();

    private static final String ARG_ERROR_MESSAGE = ErrorDialogFragment.class.getName() + ".ARG_ERROR_MESSAGE";
    private static final String ARG_ERROR_TYPE = ErrorDialogFragment.class.getName() + ".ARG_ERROR_TYPE";
    private static final String ARG_ERROR_CONTEXT = ErrorDialogFragment.class.getName() + ".ARG_ERROR_CONTEXT";
    private static final String ARG_ERROR_OPERATION = ErrorDialogFragment.class.getName() + ".ARG_ERROR_OPERATION";

    private static final String ARG_SHOW_SUPPORT_LINK = ErrorDialogFragment.class.getName() + ".ARG_SHOW_SUPPORT_LINK";
    private static final String ARG_FATAL_MESSAGE_RES = ErrorDialogFragment.class.getName() + ".ARG_FATAL_MESSAGE_RES";

    private static final String ARG_ACTION_INTENT = ErrorDialogFragment.class.getName() + ".ARG_ACTION_INTENT";
    private static final String ARG_ACTION_TITLE_RES = ErrorDialogFragment.class.getName() + ".ARG_ACTION_TITLE_RES";


    //region Creation

    public static ErrorDialogFragment presentError(@NonNull FragmentManager fm, @Nullable Throwable e) {
        ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(e);
        fragment.show(fm, TAG);
        return fragment;
    }

    public static ErrorDialogFragment presentBluetoothError(@NonNull FragmentManager fm, @NonNull Throwable e) {
        ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(e);
        if (BluetoothError.isFatal(e)) {
            Intent intent = new Intent(SenseApplication.getInstance(), SupportActivity.class);
            intent.putExtras(SupportActivity.getArguments(UserSupport.DeviceIssue.UNSTABLE_BLUETOOTH.getUri()));
            dialogFragment.setAction(intent, R.string.action_more_info);
            dialogFragment.setAddendum(R.string.error_addendum_unstable_stack);
        }
        dialogFragment.setShowSupportLink(true);
        dialogFragment.show(fm, TAG);

        return dialogFragment;
    }

    public static ErrorDialogFragment newInstance(@Nullable Throwable e) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_ERROR_MESSAGE, Errors.getDisplayMessage(e));
        arguments.putString(ARG_ERROR_TYPE, Errors.getType(e));
        arguments.putString(ARG_ERROR_CONTEXT, Errors.getContextInfo(e));
        fragment.setArguments(arguments);

        return fragment;
    }

    public static ErrorDialogFragment newInstance(@NonNull String message) {
        return newInstance(Errors.Message.from(message));
    }

    public static ErrorDialogFragment newInstance(@StringRes int messageRes) {
        return newInstance(Errors.Message.from(messageRes));
    }

    public static ErrorDialogFragment newInstance(@NonNull Errors.Message message) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_ERROR_MESSAGE, message);
        fragment.setArguments(arguments);

        return fragment;
    }

    //endregion


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        boolean isFatal = getArguments().containsKey(ARG_FATAL_MESSAGE_RES);
        if (isFatal) {
            dialog.setTitle(R.string.dialog_error_title_fatal);
            dialog.setTitleColor(getResources().getColor(R.color.destructive_accent));
        } else {
            dialog.setTitle(R.string.dialog_error_title);
        }

        CharSequence message = generateDisplayMessage();
        dialog.setMessage(message);
        Analytics.trackError(message.toString(), getErrorType(), getErrorContext(), getErrorOperation());

        if (showSupportLink()) {
            SpannableStringBuilder footer = Styles.resolveSupportLinks(getActivity(), getText(R.string.error_addendum_support));
            footer.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, footer.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            footer.insert(0, dialog.getMessage());
            dialog.setMessage(footer);
        }

        if (getTargetFragment() != null) {
            dialog.setNegativeButton(R.string.action_retry, (button, which) -> getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null));
        }

        dialog.setPositiveButton(android.R.string.ok, null);

        if (getArguments().containsKey(ARG_ACTION_INTENT) && getArguments().containsKey(ARG_ACTION_TITLE_RES)) {
            int titleRes = getArguments().getInt(ARG_ACTION_TITLE_RES);
            dialog.setNegativeButton(titleRes, (button, which) -> {
                Intent intent = getArguments().getParcelable(ARG_ACTION_INTENT);
                startActivity(intent);
            });
        }

        return dialog;
    }

    private CharSequence generateDisplayMessage() {
        CharSequence message;
        Errors.Message errorMessage = getErrorMessage();
        if (errorMessage != null) {
            message = errorMessage.resolve(getActivity());
        } else {
            message = getString(R.string.dialog_error_generic_message);
        }

        if (getArguments().containsKey(ARG_FATAL_MESSAGE_RES)) {
            SpannableStringBuilder messageBuilder = new SpannableStringBuilder(message);
            int fatalMessageRes = getArguments().getInt(ARG_FATAL_MESSAGE_RES);
            messageBuilder.append(getText(fatalMessageRes));
            return messageBuilder;
        } else {
            return message;
        }
    }

    //endregion

    //region Errors

    private void setAddendum(@StringRes int messageRes) {
        getArguments().putInt(ARG_FATAL_MESSAGE_RES, messageRes);
    }

    private @Nullable Errors.Message getErrorMessage() {
        return getArguments().getParcelable(ARG_ERROR_MESSAGE);
    }

    private @Nullable String getErrorContext() {
        return getArguments().getString(ARG_ERROR_CONTEXT);
    }

    private @Nullable String getErrorType() {
        return getArguments().getString(ARG_ERROR_TYPE);
    }

    public @Nullable String getErrorOperation() {
        return getArguments().getString(ARG_ERROR_OPERATION);
    }

    public void setErrorOperation(@Nullable String errorType) {
        getArguments().putString(ARG_ERROR_OPERATION, errorType);
    }

    //endregion


    //region Actions

    public boolean showSupportLink() {
        return getArguments().getBoolean(ARG_SHOW_SUPPORT_LINK, false);
    }

    public void setShowSupportLink(boolean show) {
        getArguments().putBoolean(ARG_SHOW_SUPPORT_LINK, show);
    }

    public void setAction(@NonNull Intent intent, @StringRes int titleRes) {
        getArguments().putParcelable(ARG_ACTION_INTENT, intent);
        getArguments().putInt(ARG_ACTION_TITLE_RES, titleRes);
    }

    //endregion
}
