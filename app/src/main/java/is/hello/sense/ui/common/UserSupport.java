package is.hello.sense.ui.common;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;

import com.segment.analytics.Properties;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.fragments.support.TicketSelectTopicFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class UserSupport {
    public static final String ORDER_URL = "https://store.hello.is";
    public static final String VIDEO_URL = "http://player.vimeo.com/external/101139949.hd.mp4?s=28ac378e29847b77e9fb7431f05d2772";
    public static final String FORGOT_PASSWORD_URL = "https://account.hello.is";

    public static Intent createViewUriIntent(@NonNull final Context context, @NonNull final Uri uri) {
        final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setShowTitle(true);
        builder.setToolbarColor(ContextCompat.getColor(context, R.color.light_accent));

        final Intent intent = builder.build().intent;
        intent.setData(uri);
        return intent;
    }

    public static void openUri(@NonNull final Context from, @NonNull final Uri uri) {
        try {
            final Intent intent = createViewUriIntent(from, uri);
            from.startActivity(intent);
        } catch (final ActivityNotFoundException | NullPointerException e) {
            final SenseAlertDialog alertDialog = new SenseAlertDialog(from);
            alertDialog.setTitle(R.string.dialog_error_title);
            alertDialog.setMessage(R.string.error_no_web_browser);
            alertDialog.setPositiveButton(android.R.string.ok, null);
            alertDialog.show();
        }
    }

    public static void showProductPage(@NonNull final Activity from) {
        String packageName = BuildConfig.APPLICATION_ID;
        if (BuildConfig.DEBUG) {
            packageName = packageName.replace(".debug", "");
        }
        try {
            final Uri marketUri = new Uri.Builder()
                    .scheme("market")
                    .appendEncodedPath("/")
                    .appendPath("details")
                    .appendQueryParameter("id", packageName)
                    .build();
            from.startActivity(new Intent(Intent.ACTION_VIEW, marketUri));
        } catch (final ActivityNotFoundException e) {
            Logger.info(UserSupport.class.getSimpleName(), "Market unavailable", e);

            final Uri webUri = new Uri.Builder()
                    .scheme("http")
                    .authority("play.google.com")
                    .appendPath("store")
                    .appendPath("apps")
                    .appendPath("details")
                    .appendQueryParameter("id", packageName)
                    .build();
            openUri(from, webUri);
        }
    }

    public static void showAmazonReviewPage(@NonNull final Activity from, @NonNull final String authority) {
        final Uri amazonReviewUri = new Uri.Builder()
                .scheme("https")
                .authority(authority)
                .appendPath("review")
                .appendPath("create-review")
                .appendQueryParameter("asin", "B016XBL2RE")
                .build();
        openUri(from, amazonReviewUri);
    }

    public static void showUserGuide(@NonNull final Activity from) {
        Analytics.trackEvent(Analytics.Backside.EVENT_HELP, null);

        final Uri supportUrl = Uri.parse("https://support.hello.is");
        openUri(from, supportUrl);
    }

    public static void showContactForm(@NonNull final Activity from) {
        Analytics.trackEvent(Analytics.Backside.EVENT_CONTACT_SUPPORT, null);

        final FragmentNavigationActivity.Builder builder = new FragmentNavigationActivity.Builder(from);
        builder.setDefaultTitle(R.string.title_select_a_topic);
        builder.setFragmentClass(TicketSelectTopicFragment.class);
        from.startActivity(builder.toIntent());
    }

    public static void showForOnboardingStep(@NonNull final Activity from, @NonNull final OnboardingStep onboardingStep) {
        final Properties properties = Analytics.createProperties(Analytics.Onboarding.PROP_HELP_STEP,
                                                                 onboardingStep.toProperty());
        Analytics.trackEvent(Analytics.Onboarding.EVENT_HELP, properties);

        openUri(from, onboardingStep.getUri());
    }

    public static void showForDeviceIssue(@NonNull final Activity from, @NonNull final DeviceIssue issue) {
        final Properties properties = Analytics.createProperties(Analytics.Backside.PROP_TROUBLESHOOTING_ISSUE,
                                                                 issue.toProperty());
        Analytics.trackEvent(Analytics.Backside.EVENT_TROUBLESHOOTING_LINK, properties);

        openUri(from, issue.getUri());
    }

    public static void showReplaceBattery(@NonNull final Activity from) {
        final Properties properties = Analytics.createProperties(Analytics.Backside.PROP_TROUBLESHOOTING_ISSUE,
                                                                 "replace-battery");
        Analytics.trackEvent(Analytics.Backside.EVENT_TROUBLESHOOTING_LINK, properties);

        final Uri issueUri = Uri.parse("https://support.hello.is/hc/en-us/articles/204496999");
        openUri(from, issueUri);
    }

    public static void showSupportedDevices(@NonNull final Activity from) {
        final Properties properties = Analytics.createProperties(Analytics.Backside.PROP_TROUBLESHOOTING_ISSUE,
                                                                 "supported-devices");
        Analytics.trackEvent(Analytics.Backside.EVENT_TROUBLESHOOTING_LINK, properties);

        final Uri issueUri = Uri.parse("https://support.hello.is/hc/en-us/articles/205152535");
        openUri(from, issueUri);
    }

    public static void showLocationPermissionMoreInfoPage(@NonNull final Activity from) {
        Analytics.trackEvent(Analytics.Permissions.EVENT_LOCATION_MORE_INFO, null);

        final Uri supportUrl = Uri.parse("https://support.hello.is/hc/en-us/articles/207716923");
        openUri(from, supportUrl);
    }

    public static void showStoragePermissionMoreInfoPage(@NonNull final Activity from) {
        Analytics.trackEvent(Analytics.Permissions.EVENT_STORAGE_MORE_INFO, null);

        final Uri supportUrl = Uri.parse("https://support.hello.is/hc/en-us/articles/209777573");
        openUri(from, supportUrl);
    }

    public static void showGalleryStoragePermissionMoreInfoPage(@NonNull final Activity from) {
        Analytics.trackEvent(Analytics.Permissions.EVENT_GALLERY_MORE_INFO, null);

        final Uri supportUrl = Uri.parse("https://support.hello.is/hc/en-us/articles/210819543");
        openUri(from, supportUrl);
    }

    public static void showFacebookAutoFillMoreInfoPage(@NonNull final Activity from) {
        final Uri supportUrl = Uri.parse("https://support.hello.is/hc/en-us/articles/210329423");
        openUri(from, supportUrl);
    }

    public static void showLearnMore(@NonNull final Context from, @StringRes final int stringRes) {
        final Uri supportUrl = Uri.parse(from.getString(stringRes));
        openUri(from, supportUrl);
    }

    public static void showAppSettings(@NonNull final Activity from) {
        try {
            from.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                          Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)));
        } catch (final ActivityNotFoundException e) {
            final SenseAlertDialog alertDialog = new SenseAlertDialog(from);
            alertDialog.setTitle(R.string.dialog_error_title);
            alertDialog.setMessage(R.string.error_no_settings_app);
            alertDialog.setPositiveButton(android.R.string.ok, null);
            alertDialog.show();
        }
    }

    public static void showUpdatePill(@NonNull final Activity from) {
        Logger.debug(UserSupport.class.getSimpleName(),"showUpdatePill()");
        from.startActivityForResult(new Intent(from, PillUpdateActivity.class), PillUpdateActivity.REQUEST_CODE);
    }

    public static void showUpdatePill(@NonNull final Fragment from) {
        Logger.debug(UserSupport.class.getSimpleName(),"showUpdatePill() from fragment " + from.getClass().getName());
        from.startActivityForResult(new Intent(from.getActivity(), PillUpdateActivity.class), PillUpdateActivity.REQUEST_CODE);
    }

    public enum DeviceIssue {
        UNSTABLE_BLUETOOTH("https://support.hello.is/hc/en-us/articles/204796429"),
        SENSE_MISSING("https://support.hello.is/hc/en-us/articles/204797259"),
        CANNOT_CONNECT_TO_SENSE("https://support.hello.is/hc/en-us/articles/205493075"),
        SENSE_NO_WIFI("https://support.hello.is/hc/en-us/articles/205493285"),
        SENSE_ASCII_WEP("https://support.hello.is/hc/en-us/articles/205019779"),
        SLEEP_PILL_MISSING("https://support.hello.is/hc/en-us/articles/204797159"),
        PAIRING_2ND_PILL("https://support.hello.is/hc/en-us/articles/204797289"),
        SLEEP_PILL_LOW_BATTERY("https://support.hello.is/hc/en-us/articles/204496999"),
        SLEEP_PILL_WEAK_RSSI("https://support.hello.is/hc/en-us/articles/211421183"),
        TIMELINE_NOT_ENOUGH_SLEEP_DATA("https://support.hello.is/hc/en-us/articles/204994629"),
        TIMELINE_NO_SLEEP_DATA("https://support.hello.is/hc/en-us/articles/205706435");

        private final String url;

        DeviceIssue(@NonNull final String url) {
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
        ADD_2ND_PILL("https://support.hello.is/hc/en-us/articles/204797289"),
        UPDATE_PILL("https://support.hello.is/hc/en-us/articles/211303163");

        private final String url;

        OnboardingStep(@NonNull final String url) {
            this.url = url;
        }

        public Uri getUri() {
            return Uri.parse(url);
        }

        public String toProperty() {
            return toString().toLowerCase();
        }

        public static OnboardingStep fromString(@Nullable final String string) {
            return Enums.fromString(string, values(), INFO);
        }
    }
}
