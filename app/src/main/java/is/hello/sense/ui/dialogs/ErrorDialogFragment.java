package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.text.SpannableStringBuilder;

import is.hello.buruberi.bluetooth.errors.BuruberiException;
import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public class ErrorDialogFragment extends SenseDialogFragment {
    public static final String TAG = ErrorDialogFragment.class.getSimpleName();

    private static final String ARG_MESSAGE = ErrorDialogFragment.class.getName() + ".ARG_MESSAGE";
    private static final String ARG_ERROR_TYPE = ErrorDialogFragment.class.getName() + ".ARG_ERROR_TYPE";
    private static final String ARG_IS_WARNING = ErrorDialogFragment.class.getName() + ".ARG_IS_WARNING";
    private static final String ARG_CONTEXT_INFO = ErrorDialogFragment.class.getName() + ".ARG_CONTEXT_INFO";
    private static final String ARG_OPERATION = ErrorDialogFragment.class.getName() + ".ARG_OPERATION";

    private static final String ARG_SHOW_SUPPORT_LINK = ErrorDialogFragment.class.getName() + ".ARG_SHOW_SUPPORT_LINK";
    private static final String ARG_ADDENDUM_RES = ErrorDialogFragment.class.getName() + ".ARG_ADDENDUM_RES";

    private static final String ARG_ACTION_INTENT = ErrorDialogFragment.class.getName() + ".ARG_ACTION_INTENT";
    private static final String ARG_ACTION_RESULT_CODE = ErrorDialogFragment.class.getName() + ".ARG_ACTION_RESULT_CODE";
    private static final String ARG_ACTION_TITLE_RES = ErrorDialogFragment.class.getName() + ".ARG_ACTION_TITLE_RES";
    private static final String ARG_TITLE_RES = ErrorDialogFragment.class.getName() + ".ARG_TITLE_RES";
    private static final String ARG_ACTION_URI_STRING = ErrorDialogFragment.class.getName() + ".ARG_ACTION_URI_STRING";


    //region Lifecycle

    public static void presentError(@NonNull final Activity activity, @Nullable final Throwable e, @StringRes final int titleRes) {
        final ErrorDialogFragment fragment = new Builder(e, activity)
                .withTitle(titleRes)
                .build();
        fragment.showAllowingStateLoss(activity.getFragmentManager(), TAG);
    }

    public static void presentError(@NonNull final Activity activity, @Nullable final Throwable e) {
        presentError(activity, e, R.string.dialog_error_title);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public
    @NonNull
    Dialog onCreateDialog(final Bundle savedInstanceState) {
        final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        final CharSequence message = generateDisplayMessage();
        dialog.setMessage(message);

        final Bundle arguments = getArguments();

        final int titleResId = arguments.getInt(ARG_TITLE_RES, R.string.dialog_error_title);
        dialog.setTitle(titleResId);


        final String errorType = arguments.getString(ARG_ERROR_TYPE);
        final String contextInfo = arguments.getString(ARG_CONTEXT_INFO);
        final String operation = arguments.getString(ARG_OPERATION);
        final boolean isWarning = arguments.getBoolean(ARG_IS_WARNING);
        trackError(message.toString(), errorType, contextInfo, operation, isWarning);

        if (getTargetFragment() != null) {
            dialog.setPositiveButton(android.R.string.ok, (ignored, which) -> {
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            });
        } else {
            dialog.setPositiveButton(android.R.string.ok, null);
        }

        if (arguments.containsKey(ARG_ACTION_TITLE_RES)) {
            final int titleRes = arguments.getInt(ARG_ACTION_TITLE_RES);
            if (arguments.containsKey(ARG_ACTION_INTENT)) {
                dialog.setNegativeButton(titleRes, (button, which) -> {
                    final Intent intent = arguments.getParcelable(ARG_ACTION_INTENT);
                    startActivity(intent);
                });
            } else if( arguments.containsKey(ARG_ACTION_URI_STRING)) {
                dialog.setNegativeButton(titleRes, (button, which) -> {
                    final Uri uri = Uri.parse(arguments.getString(ARG_ACTION_URI_STRING));
                    UserSupport.openUri(getActivity(), uri);
                });
            }else {
                dialog.setNegativeButton(titleRes, (button, which) -> {
                    if (getTargetFragment() != null) {
                        final int resultCode = arguments.getInt(ARG_ACTION_RESULT_CODE);
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
    @VisibleForTesting
    void trackError(@NonNull final String message,
                    @Nullable final String errorType,
                    @Nullable final String errorContext,
                    @Nullable final String errorOperation,
                    final boolean isWarning) {

        Analytics.trackError(message, errorType, errorContext, errorOperation, isWarning);
    }

    @VisibleForTesting
    CharSequence generateDisplayMessage() {
        final Bundle arguments = getArguments();

        CharSequence message;
        final StringRef errorMessage = arguments.getParcelable(ARG_MESSAGE);
        if (errorMessage != null) {
            message = errorMessage.resolve(getActivity());
        } else {
            message = getString(R.string.dialog_error_generic_message);
        }

        if (arguments.containsKey(ARG_ADDENDUM_RES)) {
            final SpannableStringBuilder plusAddendum = new SpannableStringBuilder(message);
            final int fatalMessageRes = arguments.getInt(ARG_ADDENDUM_RES);
            plusAddendum.append(getText(fatalMessageRes));
            message = plusAddendum;
        }

        if (arguments.getBoolean(ARG_SHOW_SUPPORT_LINK, false)) {
            final SpannableStringBuilder plusSupportLink = Styles.resolveSupportLinks(getActivity(),
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

        public Builder(@Nullable final Throwable e, @NonNull final Context context) {
            withMessage(Errors.getDisplayMessage(e));
            withErrorType(Errors.getType(e));
            withContextInfo(Errors.getContextInfo(e));
            withWarning(ApiException.isNetworkError(e));

            if (BuruberiException.isInstabilityLikely(e)) {
                withUnstableBluetoothHelp(context);
            }
        }

        public Builder withTitle(@StringRes final int titleRes) {
            arguments.putInt(ARG_TITLE_RES, titleRes);
            return this;
        }

        public Builder withMessage(@Nullable final StringRef message) {
            arguments.putParcelable(ARG_MESSAGE, message);
            return this;
        }

        public Builder withErrorType(@Nullable final String type) {
            arguments.putString(ARG_ERROR_TYPE, type);
            return this;
        }

        public Builder withWarning(final boolean isWarning) {
            arguments.putBoolean(ARG_IS_WARNING, isWarning);
            return this;
        }

        public Builder withContextInfo(@Nullable final String contextInfo) {
            arguments.putString(ARG_CONTEXT_INFO, contextInfo);
            return this;
        }

        public Builder withOperation(@Nullable final String operation) {
            arguments.putString(ARG_OPERATION, operation);
            return this;
        }

        public Builder withSupportLink() {
            arguments.putBoolean(ARG_SHOW_SUPPORT_LINK, true);
            return this;
        }

        public Builder withAddendum(@StringRes final int messageRes) {
            arguments.putInt(ARG_ADDENDUM_RES, messageRes);
            return this;
        }

        public Builder withAction(@NonNull final Intent intent, @StringRes final int titleRes) {
            arguments.putParcelable(ARG_ACTION_INTENT, intent);
            arguments.putInt(ARG_ACTION_TITLE_RES, titleRes);
            return this;
        }

        public Builder withAction(final int resultCode, @StringRes final int titleRes) {
            arguments.putInt(ARG_ACTION_RESULT_CODE, resultCode);
            arguments.putInt(ARG_ACTION_TITLE_RES, titleRes);
            return this;
        }

        public Builder withAction(@NonNull final String uriString, @StringRes final int titleRes){
            arguments.putString(ARG_ACTION_URI_STRING, uriString);
            arguments.putInt(ARG_ACTION_TITLE_RES, titleRes);
            return this;
        }

        public Builder withUnstableBluetoothHelp(@NonNull final Context context) {
            final Uri uri = UserSupport.DeviceIssue.UNSTABLE_BLUETOOTH.getUri();
            final Intent intent = UserSupport.createViewUriIntent(context, uri);
            withAction(intent, R.string.action_more_info);
            withAddendum(R.string.error_addendum_unstable_stack);
            return this;
        }

        public ErrorDialogFragment build() {
            final ErrorDialogFragment instance = new ErrorDialogFragment();
            instance.setArguments(arguments);
            return instance;
        }
    }
}
