package is.hello.sense.ui.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.model.Enums;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.SessionLogger;

public class UserSupport {
    public static final String ORDER_URL = "https://order.hello.is";

    public static final String FORGOT_PASSWORD_URL = "https://hello.is/forgot";

    public static final String SUPPORT_URL = "https://support.hello.is";
    public static final String SUPPORT_EMAIL = "help@sayhello.com";

    public static void openUrl(@NonNull Context from, @NonNull String url) {
        try {
            from.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            SenseAlertDialog alertDialog = new SenseAlertDialog(from);
            alertDialog.setTitle(R.string.dialog_error_title);
            alertDialog.setMessage(R.string.error_no_web_browser);
            alertDialog.setPositiveButton(android.R.string.ok, null);
            alertDialog.show();
        }
    }

    public static void showSupport(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_HELP, null);
        openUrl(from, SUPPORT_URL);
    }

    public static void showEmail(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_CONTACT_SUPPORT, null);
        try {
            Intent email = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", SUPPORT_EMAIL, null));
            email.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(SessionLogger.getLogFilePath(from))));
            email.putExtra(Intent.EXTRA_SUBJECT, from.getString(R.string.support_email_subject));
            email.putExtra(Intent.EXTRA_TEXT, from.getString(
                    R.string.support_email_body,
                    from.getString(R.string.app_name),
                    BuildConfig.VERSION_NAME,
                    Build.VERSION.RELEASE,
                    Build.MODEL
            ));
            email.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            from.startActivity(email);
        } catch (ActivityNotFoundException e) {
            SenseAlertDialog alertDialog = new SenseAlertDialog(from);
            alertDialog.setTitle(R.string.dialog_error_title);
            alertDialog.setMessage(R.string.error_no_email_client);
            alertDialog.setPositiveButton(android.R.string.ok, null);
            alertDialog.show();
        }
    }

    public static void showForOnboardingStep(@NonNull Context from, @NonNull OnboardingStep onboardingStep) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_HELP, Analytics.createProperties(Analytics.Onboarding.PROP_HELP_STEP, onboardingStep.toProperty()));
        openUrl(from, SUPPORT_URL);
    }

    public static void showForDeviceIssue(@NonNull Context from, @NonNull DeviceIssue issue) {
        Analytics.trackEvent(Analytics.TopView.EVENT_TROUBLESHOOTING_LINK, Analytics.createProperties(Analytics.TopView.PROP_TROUBLESHOOTING_ISSUE, issue.toProperty()));
        openUrl(from, SUPPORT_URL);
    }


    public static enum DeviceIssue {
        UNSTABLE_BLUETOOTH,
        SENSE_MISSING,
        SLEEP_PILL_MISSING,
        REPLACE_BATTERY;

        public String toProperty() {
            return toString().toLowerCase();
        }
    }

    public static enum OnboardingStep {
        INFO,
        BLUETOOTH,
        ENHANCED_AUDIO,
        SETUP_SENSE,
        WIFI_SCAN,
        SIGN_INTO_WIFI,
        PILL_PAIRING,
        PILL_PLACEMENT,
        UNSUPPORTED_DEVICE;

        public String toProperty() {
            return toString().toLowerCase();
        }

        public static OnboardingStep fromString(@Nullable String string) {
            return Enums.fromString(string, values(), INFO);
        }
    }
}
