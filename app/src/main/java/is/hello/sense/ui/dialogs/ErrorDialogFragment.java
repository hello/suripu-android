package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.text.SpannableStringBuilder;

import is.hello.buruberi.bluetooth.errors.BluetoothError;
import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public class ErrorDialogFragment extends SenseDialogFragment {
    public static final String TAG = ErrorDialogFragment.class.getSimpleName();

    private static final String ARG_MESSAGE = ErrorDialogFragment.class.getName() + ".ARG_MESSAGE";
    private static final String ARG_ERROR_TYPE = ErrorDialogFragment.class.getName() + ".ARG_ERROR_TYPE";
    private static final String ARG_CONTEXT_INFO = ErrorDialogFragment.class.getName() + ".ARG_CONTEXT_INFO";
    private static final String ARG_OPERATION = ErrorDialogFragment.class.getName() + ".ARG_OPERATION";

    private static final String ARG_SHOW_SUPPORT_LINK = ErrorDialogFragment.class.getName() + ".ARG_SHOW_SUPPORT_LINK";
    private static final String ARG_ADDENDUM_RES = ErrorDialogFragment.class.getName() + ".ARG_ADDENDUM_RES";

    private static final String ARG_ACTION_INTENT = ErrorDialogFragment.class.getName() + ".ARG_ACTION_INTENT";
    private static final String ARG_ACTION_RESULT_CODE = ErrorDialogFragment.class.getName() + ".ARG_ACTION_RESULT_CODE";
    private static final String ARG_ACTION_TITLE_RES = ErrorDialogFragment.class.getName() + ".ARG_ACTION_TITLE_RES";


    //region Lifecycle

    public static void presentError(@NonNull Activity activity, @Nullable Throwable e) {
        final ErrorDialogFragment fragment = new Builder(e, activity.getResources()).build();
        fragment.showAllowingStateLoss(activity.getFragmentManager(), TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setTitle(R.string.dialog_error_title);

        CharSequence message = generateDisplayMessage();
        dialog.setMessage(message);

        Bundle arguments = getArguments();

        String errorType = arguments.getString(ARG_ERROR_TYPE);
        String contextInfo = arguments.getString(ARG_CONTEXT_INFO);
        String operation = arguments.getString(ARG_OPERATION);
        trackError(message.toString(), errorType, contextInfo, operation);

        if (getTargetFragment() != null) {
            dialog.setPositiveButton(android.R.string.ok, (ignored, which) -> {
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            });
        } else {
            dialog.setPositiveButton(android.R.string.ok, null);
        }

        if (arguments.containsKey(ARG_ACTION_TITLE_RES)) {
            int titleRes = arguments.getInt(ARG_ACTION_TITLE_RES);
            if (arguments.containsKey(ARG_ACTION_INTENT)) {
                dialog.setNegativeButton(titleRes, (button, which) -> {
                    Intent intent = arguments.getParcelable(ARG_ACTION_INTENT);
                    startActivity(intent);
                });
            } else {
                dialog.setNegativeButton(titleRes, (button, which) -> {
                    if (getTargetFragment() != null) {
                        int resultCode = arguments.getInt(ARG_ACTION_RESULT_CODE);
                        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, null);
                    }
                });
            }
        }

        return dialog;
    }

    //endregion


    //region Internal

    /**
     * Hook for test case.
     */
    @VisibleForTesting void trackError(@NonNull String message,
                                       @Nullable String errorType,
                                       @Nullable String errorContext,
                                       @Nullable String errorOperation) {
        Analytics.trackError(message, errorType, errorContext, errorOperation);
    }

    @VisibleForTesting CharSequence generateDisplayMessage() {
        Bundle arguments = getArguments();

        CharSequence message;
        StringRef errorMessage = arguments.getParcelable(ARG_MESSAGE);
        if (errorMessage != null) {
            message = errorMessage.resolve(getActivity());
        } else {
            message = getString(R.string.dialog_error_generic_message);
        }

        if (arguments.containsKey(ARG_ADDENDUM_RES)) {
            SpannableStringBuilder plusAddendum = new SpannableStringBuilder(message);
            int fatalMessageRes = arguments.getInt(ARG_ADDENDUM_RES);
            plusAddendum.append(getText(fatalMessageRes));
            message = plusAddendum;
        }

        if (arguments.getBoolean(ARG_SHOW_SUPPORT_LINK, false)) {
            SpannableStringBuilder plusSupportLink = Styles.resolveSupportLinks(getActivity(),
                    getText(R.string.error_addendum_support));
            plusSupportLink.insert(0, message);
            message = plusSupportLink;
        }

        return message;
    }

    //endregion


    public static class Builder {
        protected final Bundle arguments = new Bundle();

        public Builder() {
        }

        public Builder(@Nullable Throwable e, @NonNull Resources resources) {
            withMessage(Errors.getDisplayMessage(e));
            withErrorType(Errors.getType(e));
            withContextInfo(Errors.getContextInfo(e));

            if (BluetoothError.isFatal(e)) {
                withUnstableBluetoothHelp(resources);
            }
        }

        public Builder withMessage(@Nullable StringRef message) {
            arguments.putParcelable(ARG_MESSAGE, message);
            return this;
        }

        public Builder withErrorType(@Nullable String type) {
            arguments.putString(ARG_ERROR_TYPE, type);
            return this;
        }

        public Builder withContextInfo(@Nullable String contextInfo) {
            arguments.putString(ARG_CONTEXT_INFO, contextInfo);
            return this;
        }

        public Builder withOperation(@Nullable String operation) {
            arguments.putString(ARG_OPERATION, operation);
            return this;
        }

        public Builder withSupportLink() {
            arguments.putBoolean(ARG_SHOW_SUPPORT_LINK, true);
            return this;
        }

        public Builder withAddendum(@StringRes int messageRes) {
            arguments.putInt(ARG_ADDENDUM_RES, messageRes);
            return this;
        }

        public Builder withAction(@NonNull Intent intent, @StringRes int titleRes) {
            arguments.putParcelable(ARG_ACTION_INTENT, intent);
            arguments.putInt(ARG_ACTION_TITLE_RES, titleRes);
            return this;
        }

        public Builder withAction(int resultCode, @StringRes int titleRes) {
            arguments.putInt(ARG_ACTION_RESULT_CODE, resultCode);
            arguments.putInt(ARG_ACTION_TITLE_RES, titleRes);
            return this;
        }

        public Builder withUnstableBluetoothHelp(@NonNull Resources resources) {
            final Uri uri = UserSupport.DeviceIssue.UNSTABLE_BLUETOOTH.getUri();
            final Intent intent = UserSupport.createViewUriIntent(resources, uri);
            withAction(intent, R.string.action_more_info);
            withAddendum(R.string.error_addendum_unstable_stack);
            return this;
        }

        public ErrorDialogFragment build() {
            ErrorDialogFragment instance = new ErrorDialogFragment();
            instance.setArguments(arguments);
            return instance;
        }
    }
}
