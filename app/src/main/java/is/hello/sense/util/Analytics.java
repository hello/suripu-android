package is.hello.sense.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import is.hello.sense.BuildConfig;
import is.hello.sense.SenseApplication;

public class Analytics {
    public static final String LOG_TAG = Analytics.class.getSimpleName();
    public static final String PLATFORM = "android";

    private static @Nullable MixpanelAPI provider;

    public interface Global {

        /**
         * iOS | android
         */
        String GLOBAL_PROP_PLATFORM = "Platform";

        /**
         * The actual Name of the user that was set upon registration
         */
        String GLOBAL_PROP_NAME = "Name";

        /**
         * The account id of the user
         */
        String GLOBAL_PROP_ACCOUNT_ID = "Account Id";

        /**
         * The id of the user's Sense.
         */
        String GLOBAL_PROP_SENSE_ID = "Sense Id";


        /**
         * Anytime an error is encountered, even if it came from server.  MAKE SURE you don't log Error in a loop ... I've seen it happen where 10,000 events get logged :)
         */
        String EVENT_ERROR = "Error";
        String PROP_ERROR_MESSAGE = "message";
        String PROP_ERROR_TYPE = "type";
        String PROP_ERROR_CONTEXT = "context";
        String PROP_ERROR_OPERATION = "operation";

        /**
         * When the user signs in
         */
        String EVENT_SIGNED_IN = "Signed In";

        /**
         * When the user signs out
         */
        String EVENT_SIGNED_OUT = "Signed Out";


        /**
         * When the user opens the app
         */
        String APP_LAUNCHED = "App Launched";


        /**
         * When the user agrees to using high power scans.
         */
        String EVENT_TURN_ON_HIGH_POWER = "High power mode enabled";
    }

    public interface Onboarding {

        /**
         * Whenever user taps on a "play" button within the Onboarding flow
         */
        String EVENT_PLAY_VIDEO = "Play Video";

        /**
         * Whenever user taps on a "skip" button within the onboarding flow
         */
        String EVENT_SKIP = "Onboarding Skip";
        String PROP_SKIP_SCREEN = "Screen";

        /**
         * Whenever user taps a "back" button within the onboarding flow.
         */
        String EVENT_BACK = "Back";

        /**
         * When the user lands on Sign In screen.
         */
        String EVENT_SIGN_IN = "Sign In Start";

        /**
         * When user lands on the Have Sense ready? screen
         */
        String EVENT_START = "Onboarding Start";

        /**
         * When user lands on the Sign Up screen
         */
        String EVENT_ACCOUNT = "Onboarding Account";

        /**
         * When user taps "I don't have a Sense" button.
         */
        String EVENT_NO_SENSE = "I don't have a Sense";

        /**
         * User lands on Birthday screen (do not log if user comes from Settings)
         */
        String EVENT_BIRTHDAY = "Onboarding Birthday";
        /**

         * User lands on Gender screen (do not log if user comes from Settings)
         */
        String EVENT_GENDER = "Onboarding Gender";
        /**

         * User lands on Height screen (do not log if user comes from Settings)
         */
        String EVENT_HEIGHT = "Onboarding Height";

        /**
         * User lands on Weight screen (do not log if user comes from Settings)
         */
        String EVENT_WEIGHT = "Onboarding Weight";

        /**
         * User lands on Location screen (do not log if user comes from Settings)
         */
        String EVENT_LOCATION = "Onboarding Location";

        /**
         * User lands on Enhanced Audio screen
         */
        String EVENT_SENSE_AUDIO = "Onboarding Sense Audio";

        /**
         * When user lands on the No BLE screen
         */
        String EVENT_NO_BLE = "Onboarding No BLE";

        /**
         * When user lands on the pairing mode help screen (not glowing purple)
         */
        String EVENT_PAIRING_MODE_HELP = "Onboarding Pairing Mode Help";

        /**
         * When user lands on the Setting up Sense screen
         */
        String EVENT_SENSE_SETUP = "Onboarding Sense Setup";

        /**
         * When user lands on the Setting up Sense screen inside the app
         */
        String EVENT_SENSE_SETUP_IN_APP = "Sense Setup";

        /**
         * When user lands on the "Pair your Sense" screen
         */
        String EVENT_PAIR_SENSE = "Onboarding Pair Sense";

        /**
         * When user lands on the "Pair your Sense" screen inside the app
         */
        String EVENT_PAIR_SENSE_IN_APP = "Pair Sense";

        /**
         * When user lands on the screen to scan for wifi
         */
        String EVENT_WIFI = "Onboarding WiFi";

        /**
         * When user lands on the screen to scan for wifi in the app
         */
        String EVENT_WIFI_IN_APP = "WiFi";

        /**
         * When the user implicitly scans for wifi networks.
         */
        String EVENT_WIFI_SCAN = "Onboarding WiFi Scan";

        /**
         * When the user implicitly scans for wifi networks in the app.
         */
        String EVENT_WIFI_SCAN_IN_APP = "WiFi Scan";

        /**
         * When the user explicitly rescans for wifi networks.
         */
        String EVENT_WIFI_RESCAN = "Onboarding WiFi Rescan";

        /**
         * When the user explicitly rescans for wifi networks in the app.
         */
        String EVENT_WIFI_RESCAN_IN_APP = "WiFi Rescan";

        /**
         * When the user lands on the "Enter Wifi Password" screen
         */
        String EVENT_WIFI_PASSWORD = "Onboarding WiFi Password";

        /**
         * When the user lands on the "Enter Wifi Password" screen in app
         */
        String EVENT_WIFI_PASSWORD_IN_APP = "WiFi Password";

        String PROP_WIFI_IS_OTHER = "Is Other";

        /**
         * When the user or the app sends WiFi credentials to Sense.
         */
        String EVENT_WIFI_CREDENTIALS_SUBMITTED = "Onboarding WiFi Credentials Submitted";

        /**
         * When the user or the app sends WiFi credentials to Sense in app.
         */
        String EVENT_WIFI_CREDENTIALS_SUBMITTED_IN_APP = "WiFi Credentials Submitted";

        String PROP_WIFI_SECURITY_TYPE = "Security Type";

        /**
         * When the user lands on the "Sleep Pill" intro screen.
         */
        String EVENT_PILL_INTRO = "Onboarding Sleep Pill";

        /**
         * When user lands on the "Pairing your Sleep Pill" screen
         */
        String EVENT_PAIR_PILL = "Onboarding Pair Pill";

        /**
         * When user lands on the "Pairing your Sleep Pill" screen inside the app
         */
        String EVENT_PAIR_PILL_IN_APP = "Pair Pill";

        /**
         * When user lands on screen where it asks user to place the pill on the pillow
         */
        String EVENT_PILL_PLACEMENT = "Onboarding Pill Placement";

        /**
         * User lands on screen which asks whether they want to pair another pill.
         */
        String EVENT_ANOTHER_PILL = "Onboarding Another Pill";

        /**
         * When user lands on the screen that tells them "To connect a second Sleep Pill, Sense needs to be put into pairing mode"
         */
        String EVENT_PAIRING_MODE_OFF = "Onboarding Pairing Mode Off";

        /**
         * When user lands on screen that tells partner to get app from hello.is/app
         */
        String EVENT_GET_APP = "Onboarding Get App";

        /**
         * When user lands on the screen that explains what the colors of Sense mean.  also known as 'before you sleep"
         */
        String EVENT_SENSE_COLORS = "Onboarding Sense Colors";

        /**
         * When user is shown the Room Check screen
         */
        String EVENT_ROOM_CHECK = "Onboarding Room Check";

        /**
         * When user is asked to set up their smart alarm during onboarding
         */
        String EVENT_FIRST_ALARM = "Onboarding First Alarm";

        /**
         * When user lands on the last onboarding Screen
         */
        String EVENT_END = "Onboarding End";

        /**
         * Whenever user taps on a "help" button
         */
        String EVENT_HELP = "Onboarding Help";
        String PROP_HELP_STEP = "onboarding_step";
    }

    public interface Timeline {
        String EVENT_TIMELINE = "Timeline";
        String PROP_DATE = "date";

        String EVENT_TIMELINE_SWIPE = "Timeline swipe";

        String EVENT_TIMELINE_OPENED = "Timeline opened";
        String EVENT_TIMELINE_CLOSED = "Timeline closed";

        String EVENT_SLEEP_SCORE_BREAKDOWN = "Sleep Score breakdown";
        String EVENT_SHARE = "Share Timeline";


        String EVENT_ZOOMED_OUT = "Timeline zoomed out";
        String EVENT_ZOOMED_IN = "Timeline zoomed in";

        String EVENT_BEFORE_SLEEP_TAPPED = "Timeline before sleep tab tapped";

        String EVENT_TAP = "Timeline tap";
        String EVENT_TIMELINE_EVENT_TAPPED = "Timeline Event tapped";
        String EVENT_LONG_PRESS_EVENT = "Long press sleep duration bar";
        String EVENT_ADJUST_TIME = "Timeline adjust time tapped";

        String EVENT_BEFORE_SLEEP_EVENT_TAPPED = "Before sleep event tapped";


        String EVENT_SYSTEM_ALERT = "System Alert";

        String PROP_SYSTEM_ALERT_TYPE = "Type";
        int SYSTEM_ALERT_TYPE_SENSE_NOT_PAIRED = 2;
        int SYSTEM_ALERT_TYPE_PILL_NOT_PAIRED = 4;
        int SYSTEM_ALERT_TYPE_PILL_LOW_BATTERY = 5;
        int SYSTEM_ALERT_TYPE_SENSE_NOT_SEEN = 6;
        int SYSTEM_ALERT_TYPE_PILL_NOT_SEEN = 7;

        String EVENT_SYSTEM_ALERT_ACTION = "System Alert Action";

        String PROP_EVENT_SYSTEM_ALERT_ACTION = "Action";
        String PROP_EVENT_SYSTEM_ALERT_ACTION_NOW = "now";
        String PROP_EVENT_SYSTEM_ALERT_ACTION_LATER = "later";
    }

    public interface TopView {
        String EVENT_TOP_VIEW = "Top View";

        String EVENT_CURRENT_CONDITIONS = "Current Conditions";
        String EVENT_SENSOR_HISTORY = "Sensor History";
        String PROP_SENSOR_NAME = "sensor_name";

        String EVENT_TRENDS = "Trends";

        String EVENT_MAIN_VIEW = "Main View";
        String EVENT_INSIGHT_DETAIL = "Insight Detail";
        String EVENT_QUESTION = "Question";
        String EVENT_SKIP_QUESTION = "Skip Question";
        String EVENT_ANSWER_QUESTION = "Answer Question";

        String EVENT_ALARMS = "Alarms";
        String EVENT_NEW_ALARM = "New Alarm";
        String EVENT_ALARM_SAVED = "Alarm Saved";
        String EVENT_ALARM_ON_OFF = "Alarm On/Off";
        String EVENT_EDIT_ALARM = "Edit Alarm";

        String EVENT_SETTINGS = "Settings";
        String EVENT_ACCOUNT = "Account";
        String EVENT_CHANGE_EMAIL = "Change email";
        String EVENT_CHANGE_PASSWORD = "Change password";

        String EVENT_DEVICES = "Devices";
        String EVENT_SENSE_DETAIL = "Sense detail";
        String EVENT_REPLACE_SENSE = "Replace Sense";
        String EVENT_PUT_INTO_PAIRING_MODE = "Put into Pairing Mode";
        String EVENT_FACTORY_RESET = "Factory Reset";
        String EVENT_EDIT_WIFI = "Edit WiFi";
        String EVENT_SENSE_ADVANCED = "Sense advanced tapped";
        String EVENT_TIME_ZONE = "Time Zone";
        String EVENT_TIME_ZONE_CHANGED = "Time Zone Changed";
        String PROP_TIME_ZONE = "tz";

        String EVENT_PILL_DETAIL = "Pill detail";
        String EVENT_REPLACE_PILL = "Replace Pill";
        String EVENT_REPLACE_BATTERY = "Replace Battery";
        String EVENT_PILL_ADVANCED = "Pill advanced tapped";

        String EVENT_TROUBLESHOOTING_LINK = "Troubleshooting link";
        String PROP_TROUBLESHOOTING_ISSUE = "issue";

        String EVENT_NOTIFICATIONS = "Notifications";
        String EVENT_UNITS_TIME = "Units/Time";
        String EVENT_SIGN_OUT = "Sign Out";

        String EVENT_HELP = "Settings Help";
        String EVENT_SEND_FEEDBACK = "Send Feedback";
        String EVENT_CONTACT_SUPPORT = "Contact Support";
    }

    public interface Widgets {
        /**
         * When the user creates their first home screen widget of a certain type.
         */
        String EVENT_CREATED = "Widget Created";
        /**
         * When the user deletes a home screen widget.
         */
        String EVENT_DELETED = "Widget Deleted";

        String PROP_NAME = "widget_name";
    }


    //region Lifecycle

    public static void initialize(@NonNull Context context) {
        if (provider == null) {
            Analytics.provider = MixpanelAPI.getInstance(context, BuildConfig.MP_API_KEY);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static void onResume(@NonNull Activity activity) {
    }

    @SuppressWarnings("UnusedParameters")
    public static void onPause(@NonNull Activity activity) {
        if (provider != null) {
            provider.flush();
        }
    }

    //endregion


    //region User Identity

    public static void trackUserIdentifier(@NonNull String userId) {
        Logger.info(Analytics.LOG_TAG, "Began session for " + userId);

        if (Crashlytics.getInstance().isInitialized()) {
            Crashlytics.setUserIdentifier(userId);
        }
    }

    public static void trackRegistration(@Nullable String accountId, @Nullable String name, @NonNull DateTime created) {
        Logger.info(LOG_TAG, "Tracking user sign up { accountId: '" + accountId + "', name: '" + name + "', created: '" + created + "' }");

        Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_IN, null);

        if (accountId == null) {
            accountId = "";
        }

        if (provider != null) {

            if (name == null) {
                name = "";
            }

            MixpanelAPI.People people = provider.getPeople();

            String distinctId = provider.getDistinctId();
            provider.alias(accountId, distinctId);
            people.identify(distinctId);

            people.set("$name", name);
            people.set("$created", created.toString());
            people.set(Global.GLOBAL_PROP_ACCOUNT_ID, accountId);
            people.set(Global.GLOBAL_PROP_PLATFORM, PLATFORM);

            provider.registerSuperProperties(createProperties(
                    Global.GLOBAL_PROP_NAME, name,
                    Global.GLOBAL_PROP_PLATFORM, PLATFORM
            ));
        }

        trackUserIdentifier(accountId);
    }

    public static void trackSignIn(@NonNull String accountId) {
        Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_IN, null);

        if (provider != null) {
            provider.identify(accountId);
            provider.getPeople().identify(accountId);
        }

        trackUserIdentifier(accountId);
    }

    public static void setSenseId(@Nullable String senseId) {
        Logger.info(LOG_TAG, "Tracking Sense " + senseId);

        if (provider != null) {
            provider.getPeople().set(Global.GLOBAL_PROP_SENSE_ID, senseId);
        }
    }

    //endregion


    //region Events

    public static @NonNull JSONObject createProperties(@NonNull Object... pairs) {
        if ((pairs.length % 2) != 0) {
            throw new IllegalArgumentException("even number of arguments required");
        }

        JSONObject properties = new JSONObject();
        try {
            for (int i = 0; i < pairs.length; i += 2) {
                properties.put(pairs[i].toString(), pairs[i + 1]);
            }
        } catch (JSONException ignored) {
        }
        return properties;
    }

    public static void trackEvent(@NonNull String event, @Nullable JSONObject properties) {
        if (provider != null) {
            provider.track(event, properties);
        }

        Logger.analytic(event, properties);
    }

    public static void trackError(@NonNull String message,
                                  @Nullable String errorType,
                                  @Nullable String errorContext,
                                  @Nullable String errorOperation) {
        JSONObject properties = createProperties(
            Global.PROP_ERROR_MESSAGE, message,
            Global.PROP_ERROR_TYPE, errorType,
            Global.PROP_ERROR_CONTEXT, errorContext,
            Global.PROP_ERROR_OPERATION, errorOperation
        );
        trackEvent(Global.EVENT_ERROR, properties);
    }

    public static void trackError(@Nullable Throwable e, @Nullable String errorOperation) {
        StringRef message = Errors.getDisplayMessage(e);
        String messageString;
        if (message != null && SenseApplication.getInstance() != null) {
            messageString = message.resolve(SenseApplication.getInstance());
        } else {
            messageString = "Unknown";
        }
        trackError(messageString, Errors.getType(e), Errors.getContextInfo(e), errorOperation);
    }

    public static void trackUnexpectedError(@Nullable Throwable e) {
        if (e != null && Crashlytics.getInstance().isInitialized()) {
            Crashlytics.logException(e);
        }
    }

    //endregion
}
