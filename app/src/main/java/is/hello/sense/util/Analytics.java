package is.hello.sense.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

public class Analytics {
    public static final String LOG_TAG = Analytics.class.getSimpleName();

    public static final String GLOBAL_PROP_PLATFORM = "Platform";
    public static final String PLATFORM = "android";

    public static final String GLOBAL_PROP_NAME = "Name";
    public static final String GLOBAL_PROP_ACCOUNT_ID = "Account Id";


    /**
     * Anytime an error is encountered, even if it came from server.  MAKE SURE you don't log Error in a loop ... I've seen it happen where 10,000 events get logged :)
     */
    public static final String EVENT_ERROR = "Error";
    public static final String PROP_ERROR_CODE = "code";
    public static final String PROP_ERROR_MESSAGE = "message";


    /**
     * Whenever user taps on a "help" button
     */
    public static final String EVENT_HELP = "Help";
    public static final String PROP_HELP_STEP = "step";


    /**
     * Whenever user taps on a "play" button within the Onboarding flow
     */
    public static final String EVENT_PLAY_VIDEO = "Play Video";

    /**
     * After user taps on Sign Up / lands on Sign Up screen
     */
    public static final String EVENT_ONBOARDING_START = "Onboarding Start";

    /**
     * User lands on Birthday screen (do not log if user comes from Settings)
     */
    public static final String EVENT_ONBOARDING_BIRTHDAY = "Onboarding Birthday";

    /**
     * User lands on Gender screen (do not log if user comes from Settings)
     */
    public static final String EVENT_ONBOARDING_GENDER = "Onboarding Gender";

    /**
     * User lands on Height screen (do not log if user comes from Settings)
     */
    public static final String EVENT_ONBOARDING_HEIGHT = "Onboarding Height";

    /**
     * User lands on Weight screen (do not log if user comes from Settings)
     */
    public static final String EVENT_ONBOARDING_WEIGHT = "Onboarding Weight";

    /**
     * When user lands on the Setting up Sense screen
     */
    public static final String EVENT_ONBOARDING_SENSE_SETUP = "Onboarding Sense Setup";

    /**
     * When user lands on the "Pair your Sense" screen
     */
    public static final String EVENT_ONBOARDING_PAIR_SENSE = "Onboarding Pair Sense";
    /**
     * When the user lands on the "Enter Wifi Password" screen
     */
    public static final String EVENT_ONBOARDING_WIFI_PASSWORD = "Onboarding WiFi Password";

    /**
     * When the user explicitly rescans for wifi networks.
     */
    public static final String EVENT_ONBOARDING_WIFI_SCAN = "Onboarding WiFi Scan";

    /**
     * When the Sense wifi scan completes.
     */
    public static final String EVENT_ONBOARDING_WIFI_SCAN_COMPLETED = "Onboarding Wifi Scan Completed";
    public static final String PROP_DURATION = "duration"; //in seconds
    public static final String PROP_FAILED = "failed";

    /**
     * When user lands on the "Pairing your Sleep Pill" screen
     */
    public static final String EVENT_ONBOARDING_PAIR_PILL = "Onboarding Pair Pill";

    /**
     * When user lands on the last onboarding Screen
     */
    public static final String EVENT_ONBOARDING_END = "Onboarding End";

    /**
     * When the user switches dates in the timeline (swipe, taps an event)
     */
    public static final String EVENT_TIMELINE_ACTION = "Timeline Action";
    public static final String PROP_TIMELINE_ACTION = "action";
    public static final String PROP_TIMELINE_ACTION_CHANGE_DATE = "change_date";
    public static final String PROP_TIMELINE_ACTION_TAP_EVENT = "tap_event";


    /**
     * When the user adds an alarm
     */
    public static final String EVENT_ALARM_ACTION = "Alarm Action";
    public static final String PROP_ALARM_ACTION = "action";
    public static final String PROP_ALARM_ACTION_ADD = "add";
    public static final String PROP_ALARM_ACTION_EDIT = "edit";
    public static final String PROP_ALARM_ACTION_DISABLE = "disable";


    /**
     * When user lands on the Sign in screen
     */
    public static final String EVENT_SIGN_IN_START = "Sign In Start";

    /**
     * When the user signs in
     */
    public static final String EVENT_SIGNED_IN = "Signed In";

    /**
     * When the user signs out
     */
    public static final String EVENT_SIGNED_OUT = "Signed Out";

    /**
     * When user takes a device action in the 'device management' area of the application
     */
    public static final String EVENT_DEVICE_ACTION = "Device Action";
    public static final String PROP_DEVICE_ACTION = "action";
    public static final String PROP_DEVICE_ACTION_FACTORY_RESTORE = "factory restore";
    public static final String PROP_DEVICE_ACTION_ENABLE_PAIRING_MODE = "enable pairing mode";


    /**
     * When the user creates their first home screen widget of a certain type.
     */
    public static final String EVENT_WIDGET_CREATED = "Widget Created";

    /**
     * When the user deletes a home screen widget.
     */
    public static final String EVENT_WIDGET_DELETED = "Widget Deleted";

    public static final String PROP_WIDGET_NAME = "widget name";


    private static MixpanelAPI mixpanel;
    private static SharedPreferences preferences;

    public static void initialize(@NonNull Context context, @NonNull String apiKey) {
        Analytics.mixpanel = MixpanelAPI.getInstance(context, apiKey);
        Analytics.preferences = context.getSharedPreferences(Constants.INTERNAL_PREFS, 0);
    }

    public static void startSession() {
    }

    public static void endSession() {
        mixpanel.flush();
    }

    public static void setUserId(@NonNull String userId) {
        String existingUserId = preferences.getString(Constants.INTERNAL_PREF_ANALYTICS_USER_ID, null);
        mixpanel.getPeople().identify(userId);
        if (existingUserId == null) {
            Logger.info(LOG_TAG, "Identifying user.");
            mixpanel.identify(userId);
        } else if (!existingUserId.equals(userId)) {
            Logger.info(LOG_TAG, "Establishing user alias.");
            mixpanel.alias(userId, existingUserId);
        }

        if (Crashlytics.getInstance().isInitialized()) {
            Crashlytics.setUserIdentifier(userId);
        }

        preferences.edit()
                   .putString(Constants.INTERNAL_PREF_ANALYTICS_USER_ID, userId)
                   .apply();
    }

    public static void trackUserSignUp(@Nullable String accountId, @Nullable String name, @NonNull DateTime created) {
        Logger.info(LOG_TAG, "Tracking user sign up { accountId: '" + accountId + "', name: '" + name + "', created: '" + created + "' }");

        if (accountId == null) {
            accountId = "";
        }

        if (name == null) {
            name = "";
        }

        mixpanel.getPeople().set("$name", name);
        mixpanel.getPeople().set("$created", created.toString());
        mixpanel.getPeople().set(GLOBAL_PROP_ACCOUNT_ID, accountId);
        mixpanel.getPeople().set(GLOBAL_PROP_PLATFORM, PLATFORM);

        mixpanel.registerSuperProperties(createProperties(
                GLOBAL_PROP_NAME, name,
                GLOBAL_PROP_PLATFORM, PLATFORM
        ));
    }

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

    public static void event(@NonNull String event, @Nullable JSONObject properties) {
        mixpanel.track(event, properties);
        Logger.info(LOG_TAG, event + ": " + properties);
    }

    public static void error(@NonNull String message, int code) {
        event(EVENT_ERROR, createProperties(PROP_ERROR_MESSAGE, message,
                                            PROP_ERROR_CODE, code));
    }
}
