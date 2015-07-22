package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.SpannableStringBuilder;

import is.hello.buruberi.bluetooth.errors.BluetoothError;
import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.ui.activities.SupportActivity;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public class ErrorDialogFragment extends SenseDialogFragment {
    public static final String TAG = ErrorDialogFragment.class.getSimpleName();

    private static final String ARG_ERROR_MESSAGE = ErrorDialogFragment.class.getName() + ".ARG_ERROR_MESSAGE";
    private static final String ARG_ERROR_TYPE = ErrorDialogFragment.class.getName() + ".ARG_ERROR_TYPE";
    private static final String ARG_ERROR_CONTEXT = ErrorDialogFragment.class.getName() + ".ARG_ERROR_CONTEXT";
    private static final String ARG_ERROR_OPERATION = ErrorDialogFragment.class.getName() + ".ARG_ERROR_OPERATION";

    private static final String ARG_SHOW_SUPPORT_LINK = ErrorDialogFragment.class.getName() + ".ARG_SHOW_SUPPORT_LINK";
    private static final String ARG_ADDENDUM_RES = ErrorDialogFragment.class.getName() + ".ARG_ADDENDUM_RES";

    private static final String ARG_ACTION_INTENT = ErrorDialogFragment.class.getName() + ".ARG_ACTION_INTENT";
    private static final String ARG_ACTION_RESULT_CODE = ErrorDialogFragment.class.getName() + ".ARG_ACTION_RESULT_CODE";
    private static final String ARG_ACTION_TITLE_RES = ErrorDialogFragment.class.getName() + ".ARG_ACTION_TITLE_RES";


    //region Creation

    public static ErrorDialogFragment presentError(@NonNull FragmentManager fm, @Nullable Throwable e) {
        ErrorDialogFragment fragment = new Builder()
                .setError(e)
                .create();
        fragment.showAllowingStateLoss(fm, TAG);
        return fragment;
    }

    public static ErrorDialogFragment presentBluetoothError(@NonNull FragmentManager fm, @NonNull Throwable e) {
        Builder builder = new Builder()
                .setError(e)
                .setShowSupportLink(true);

        if (BluetoothError.isFatal(e)) {
            Intent intent = new Intent(SenseApplication.getInstance(), SupportActivity.class);
            intent.putExtras(SupportActivity.getArguments(UserSupport.DeviceIssue.UNSTABLE_BLUETOOTH.getUri()));
            builder.setAction(intent, R.string.action_more_info);
            builder.setAddendum(R.string.error_addendum_unstable_stack);
        }

        ErrorDialogFragment dialogFragment = builder.create();
        dialogFragment.showAllowingStateLoss(fm, TAG);
        return dialogFragment;
    }

    @Deprecated
    public static ErrorDialogFragment newInstance(@Nullable Throwable e) {
        return new Builder()
                .setError(e)
                .create();
    }

    @Deprecated
    public static ErrorDialogFragment newInstance(@NonNull String message) {
        return new Builder()
                .setMessage(StringRef.from(message))
                .create();
    }

    @Deprecated
    public static ErrorDialogFragment newInstance(@StringRes int messageRes) {
        return new Builder()
                .setMessage(StringRef.from(messageRes))
                .create();
    }

    @Deprecated
    public static ErrorDialogFragment newInstance(@NonNull StringRef message) {
        return new Builder()
                .setMessage(message)
                .create();
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

        boolean isFatal = getArguments().containsKey(ARG_ADDENDUM_RES);
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
            footer.insert(0, dialog.getMessage());
            dialog.setMessage(footer);
        }

        if (getTargetFragment() != null) {
            dialog.setNegativeButton(android.R.string.ok, (ignored, which) -> {
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            });
        } else {
            dialog.setPositiveButton(android.R.string.ok, null);
        }

        if (getArguments().containsKey(ARG_ACTION_TITLE_RES)) {
            int titleRes = getArguments().getInt(ARG_ACTION_TITLE_RES);
            if (getArguments().containsKey(ARG_ACTION_INTENT)) {
                dialog.setNegativeButton(titleRes, (button, which) -> {
                    Intent intent = getArguments().getParcelable(ARG_ACTION_INTENT);
                    startActivity(intent);
                });
            } else {
                dialog.setNegativeButton(titleRes, (button, which) -> {
                    if (getTargetFragment() != null) {
                        int resultCode = getArguments().getInt(ARG_ACTION_RESULT_CODE);
                        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, null);
                    }
                });
            }
        }

        return dialog;
    }

    private CharSequence generateDisplayMessage() {
        CharSequence message;
        StringRef errorMessage = getErrorMessage();
        if (errorMessage != null) {
            message = errorMessage.resolve(getActivity());
        } else {
            message = getString(R.string.dialog_error_generic_message);
        }

        if (getArguments().containsKey(ARG_ADDENDUM_RES)) {
            SpannableStringBuilder messageBuilder = new SpannableStringBuilder(message);
            int fatalMessageRes = getArguments().getInt(ARG_ADDENDUM_RES);
            messageBuilder.append(getText(fatalMessageRes));
            return messageBuilder;
        } else {
            return message;
        }
    }

    //endregion

    //region Errors

    @Deprecated
    private void setAddendum(@StringRes int messageRes) {
        getArguments().putInt(ARG_ADDENDUM_RES, messageRes);
    }

    private @Nullable StringRef getErrorMessage() {
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

    @Deprecated
    public void setErrorOperation(@Nullable String errorType) {
        getArguments().putString(ARG_ERROR_OPERATION, errorType);
    }

    //endregion


    //region Actions

    public boolean showSupportLink() {
        return getArguments().getBoolean(ARG_SHOW_SUPPORT_LINK, false);
    }

    @Deprecated
    public void setShowSupportLink(boolean show) {
        getArguments().putBoolean(ARG_SHOW_SUPPORT_LINK, show);
    }

    @Deprecated
    public void setAction(@NonNull Intent intent, @StringRes int titleRes) {
        getArguments().putParcelable(ARG_ACTION_INTENT, intent);
        getArguments().putInt(ARG_ACTION_TITLE_RES, titleRes);
    }

    @Deprecated
    public void setAction(int resultCode, @StringRes int titleRes) {
        getArguments().putInt(ARG_ACTION_RESULT_CODE, resultCode);
        getArguments().putInt(ARG_ACTION_TITLE_RES, titleRes);
    }

    //endregion


    public static class Builder {
        private final Bundle arguments = new Bundle();

        public Builder setError(Throwable e) {
            setMessage(Errors.getDisplayMessage(e));
            setType(Errors.getType(e));
            setContextInfo(Errors.getContextInfo(e));
            return this;
        }

        public Builder setMessage(StringRef message) {
            arguments.putParcelable(ARG_ERROR_MESSAGE, message);
            return this;
        }

        public Builder setType(String type) {
            arguments.putString(ARG_ERROR_TYPE, type);
            return this;
        }

        public Builder setContextInfo(String contextInfo) {
            arguments.putString(ARG_ERROR_CONTEXT, contextInfo);
            return this;
        }

        public Builder setOperation(String operation) {
            arguments.putString(ARG_ERROR_OPERATION, operation);
            return this;
        }

        public Builder setShowSupportLink(boolean show) {
            arguments.putBoolean(ARG_SHOW_SUPPORT_LINK, show);
            return this;
        }

        public Builder setAddendum(@StringRes int messageRes) {
            arguments.putInt(ARG_ADDENDUM_RES, messageRes);
            return this;
        }

        public Builder setAction(@NonNull Intent intent, @StringRes int titleRes) {
            arguments.putParcelable(ARG_ACTION_INTENT, intent);
            arguments.putInt(ARG_ACTION_TITLE_RES, titleRes);
            return this;
        }

        public Builder setAction(int resultCode, @StringRes int titleRes) {
            arguments.putInt(ARG_ACTION_RESULT_CODE, resultCode);
            arguments.putInt(ARG_ACTION_TITLE_RES, titleRes);
            return this;
        }

        public ErrorDialogFragment create() {
            ErrorDialogFragment instance = new ErrorDialogFragment();
            instance.setArguments(arguments);
            return instance;
        }
    }
}
