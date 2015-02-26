package is.hello.sense.ui.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
    public static final String FORGOT_PASSWORD_URL = "https://account.hello.is";
    public static final String SUPPORT_EMAIL = "support@hello.is";
    public static final String FEEDBACK_EMAIL = "feedback@hello.is";

    public static void openUri(@NonNull Context from, @NonNull Uri uri) {
        try {
            from.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            SenseAlertDialog alertDialog = new SenseAlertDialog(from);
            alertDialog.setTitle(R.string.dialog_error_title);
            alertDialog.setMessage(R.string.error_no_web_browser);
            alertDialog.setPositiveButton(android.R.string.ok, null);
            alertDialog.show();
        }
    }

    public static void showEmailComposer(@NonNull Context from,
                                         @NonNull String emailAddress,
                                         @NonNull Bundle extras) {
        try {
            Intent email = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", emailAddress, null));
            email.putExtras(extras);
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

    public static void showSupport(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_HELP, null);
        openUri(from, Uri.fromParts("http", BuildConfig.SUPPORT_AUTHORITY, null));
    }

    public static void showEmailSupport(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_CONTACT_SUPPORT, null);

        Bundle extras = new Bundle();
        extras.putParcelable(Intent.EXTRA_STREAM, Uri.fromFile(new File(SessionLogger.getLogFilePath(from))));
        extras.putString(Intent.EXTRA_SUBJECT, from.getString(R.string.support_email_subject));
        extras.putString(Intent.EXTRA_TEXT, from.getString(
                R.string.support_email_body,
                from.getString(R.string.app_name),
                BuildConfig.VERSION_NAME,
                Build.VERSION.RELEASE,
                Build.MODEL
        ));
        showEmailComposer(from, SUPPORT_EMAIL, extras);
    }

    public static void showEmailFeedback(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_SEND_FEEDBACK, null);

        Bundle extras = new Bundle();
        extras.putString(Intent.EXTRA_SUBJECT, from.getString(R.string.feedback_email_subject_fmt, BuildConfig.VERSION_NAME));
        showEmailComposer(from, FEEDBACK_EMAIL, extras);
    }

    public static void showForOnboardingStep(@NonNull Context from, @NonNull OnboardingStep onboardingStep) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_HELP, Analytics.createProperties(Analytics.Onboarding.PROP_HELP_STEP, onboardingStep.toProperty()));
        openUri(from, onboardingStep.getUri());
    }

    public static void showForDeviceIssue(@NonNull Context from, @NonNull DeviceIssue issue) {
        Analytics.trackEvent(Analytics.TopView.EVENT_TROUBLESHOOTING_LINK, Analytics.createProperties(Analytics.TopView.PROP_TROUBLESHOOTING_ISSUE, issue.toProperty()));
        openUri(from, issue.getUri());
    }

    public static void showEnhancedAudio(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_TROUBLESHOOTING_LINK, Analytics.createProperties(Analytics.TopView.PROP_TROUBLESHOOTING_ISSUE, "enhanced-audio"));

        Uri issueUri = new Uri.Builder()
                .scheme("http")
                .authority(BuildConfig.SUPPORT_AUTHORITY)
                .appendPath("app")
                .appendPath("enhanced-audio")
                .build();
        openUri(from, issueUri);
    }

    public static void showReplaceBattery(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_TROUBLESHOOTING_LINK, Analytics.createProperties(Analytics.TopView.PROP_TROUBLESHOOTING_ISSUE, "enhanced-audio"));

        Uri issueUri = new Uri.Builder()
                .scheme("http")
                .authority(BuildConfig.SUPPORT_AUTHORITY)
                .appendPath("sleep-pill")
                .appendPath("battery-change")
                .build();
        openUri(from, issueUri);
    }


    private static Uri buildSupportUrl(@NonNull String slug) {
        return new Uri.Builder()
                .scheme("http")
                .authority(BuildConfig.SUPPORT_AUTHORITY)
                .appendPath("troubleshoot")
                .appendPath(slug)
                .build();
    }

    public static enum DeviceIssue {
        UNSTABLE_BLUETOOTH("android-bluetooth-problems"),
        SENSE_MISSING("sense-not-seen-in-days"),
        CANNOT_CONNECT_TO_SENSE("cannot-connect-sense-ble"),
        SENSE_NO_WIFI("sense-cannot-connect-internet"),
        SLEEP_PILL_MISSING("pill-not-seen-in-days"),
        PAIRING_2ND_PILL("setting-up-second-sleep-pill");

        private final String slug;
        private DeviceIssue(@NonNull String slug) {
            this.slug = slug;
        }

        public Uri getUri() {
            return buildSupportUrl(slug);
        }

        public String toProperty() {
            return toString().toLowerCase();
        }
    }

    public static enum OnboardingStep {
        INFO(""),
        DEMOGRAPHIC_QUESTIONS("demographic-questions"),
        BLUETOOTH("turning-on-bluetooth"),
        ENHANCED_AUDIO("enhanced-audio"),
        SETTING_UP_SENSE("setting-up-sense"),
        PAIRING_MODE("pairing-mode"),
        PAIRING_SENSE_BLE("pairing-sense-ble"),
        WIFI_SCAN("connecting-sense-wifi"),
        SIGN_INTO_WIFI("connecting-sense-wifi"),
        PILL_PAIRING("pairing-your-sleep-pill"),
        PILL_PLACEMENT("attaching-sleep-pill"),
        ADD_2ND_PILL("setting-up-second-sleep-pill");

        private final String slug;
        private OnboardingStep(@NonNull String slug) {
            this.slug = slug;
        }

        public Uri getUri() {
            return buildSupportUrl(slug);
        }

        public String toProperty() {
            return toString().toLowerCase();
        }

        public static OnboardingStep fromString(@Nullable String string) {
            return Enums.fromString(string, values(), INFO);
        }
    }
}
