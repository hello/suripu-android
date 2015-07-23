package is.hello.sense.ui.common;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.ui.activities.SupportActivity;
import is.hello.sense.ui.fragments.support.ContactTopicFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;

public class UserSupport {
    public static final String ORDER_URL = "https://order.hello.is";
    public static final String FORGOT_PASSWORD_URL = "https://account.hello.is";

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

    public static void showUserGuide(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_HELP, null);

        Uri supportUrl = Uri.parse("https://support.hello.is");
        from.startActivity(SupportActivity.getIntent(from, supportUrl));
    }

    public static void showContactForm(@NonNull Activity from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_CONTACT_SUPPORT, null);

        Intent intent = new Intent(from, FragmentNavigationActivity.class);
        intent.putExtras(FragmentNavigationActivity.getArguments(
                from.getString(R.string.title_select_a_topic),
                ContactTopicFragment.class,
                null));
        from.startActivity(intent);
    }

    public static void showForOnboardingStep(@NonNull Context from, @NonNull OnboardingStep onboardingStep) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_HELP, Analytics.createProperties(Analytics.Onboarding.PROP_HELP_STEP, onboardingStep.toProperty()));
        from.startActivity(SupportActivity.getIntent(from, onboardingStep.getUri()));
    }

    public static void showForDeviceIssue(@NonNull Context from, @NonNull DeviceIssue issue) {
        Analytics.trackEvent(Analytics.TopView.EVENT_TROUBLESHOOTING_LINK, Analytics.createProperties(Analytics.TopView.PROP_TROUBLESHOOTING_ISSUE, issue.toProperty()));
        from.startActivity(SupportActivity.getIntent(from, issue.getUri()));
    }

    public static void showReplaceBattery(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_TROUBLESHOOTING_LINK, Analytics.createProperties(Analytics.TopView.PROP_TROUBLESHOOTING_ISSUE, "enhanced-audio"));

        Uri issueUri = Uri.parse("https://support.hello.is/hc/en-us/articles/204496999");
        from.startActivity(SupportActivity.getIntent(from, issueUri));
    }

    public static void showSupportedDevices(@NonNull Context from) {
        Analytics.trackEvent(Analytics.TopView.EVENT_TROUBLESHOOTING_LINK, Analytics.createProperties(Analytics.TopView.PROP_TROUBLESHOOTING_ISSUE, "enhanced-audio"));

        Uri issueUri = Uri.parse("https://support.hello.is/hc/en-us/articles/205152535");
        from.startActivity(SupportActivity.getIntent(from, issueUri));
    }


    public enum DeviceIssue {
        UNSTABLE_BLUETOOTH("https://support.hello.is/hc/en-us/articles/204796429"),
        SENSE_MISSING("https://support.hello.is/hc/en-us/articles/204797259"),
        CANNOT_CONNECT_TO_SENSE("https://support.hello.is/hc/en-us/articles/205493075"),
        SENSE_NO_WIFI("https://support.hello.is/hc/en-us/articles/205493285"),
        SLEEP_PILL_MISSING("https://support.hello.is/hc/en-us/articles/204797159"),
        PAIRING_2ND_PILL("https://support.hello.is/hc/en-us/articles/204797289");

        private final String url;
        DeviceIssue(@NonNull String url) {
            this.url = url;
        }

        public Uri getUri() {
            return Uri.parse(url);
        }

        public String toProperty() {
            return toString().toLowerCase();
        }
    }

    public enum OnboardingStep {
        INFO(""),
        DEMOGRAPHIC_QUESTIONS("https://support.hello.is/hc/en-us/articles/204796959"),
        BLUETOOTH("https://support.hello.is/hc/en-us/articles/205493335"),
        ENHANCED_AUDIO("https://support.hello.is/hc/en-us/articles/204812619"),
        SETTING_UP_SENSE("https://support.hello.is/hc/en-us/articles/204797299"),
        PAIRING_MODE("https://support.hello.is/hc/en-us/articles/204797179"),
        PAIRING_SENSE_BLE("https://support.hello.is/hc/en-us/articles/205493235"),
        WIFI_SCAN("https://support.hello.is/hc/en-us/articles/205493095"),
        SIGN_INTO_WIFI("https://support.hello.is/hc/en-us/articles/205493095"),
        PILL_PAIRING("https://support.hello.is/hc/en-us/articles/204797129"),
        PILL_PLACEMENT("https://support.hello.is/hc/en-us/articles/205493045"),
        ADD_2ND_PILL("https://support.hello.is/hc/en-us/articles/204797289");

        private final String url;
        OnboardingStep(@NonNull String url) {
            this.url = url;
        }

        public Uri getUri() {
            return Uri.parse(url);
        }

        public String toProperty() {
            return toString().toLowerCase();
        }

        public static OnboardingStep fromString(@Nullable String string) {
            return Enums.fromString(string, values(), INFO);
        }
    }
}
