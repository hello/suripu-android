package is.hello.sense.util;

import android.app.Activity;
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

    private static @Nullable MixpanelAPI provider;
    private static @Nullable SharedPreferences preferences;


    //region Global Properties

    /**
     * iOS | android
     */
    public static final String GLOBAL_PROP_PLATFORM = "Platform";
    public static final String PLATFORM = "android";

    /**
     * The actual Name of the user that was set upon registration
     */
    public static final String GLOBAL_PROP_NAME = "Name";

    /**
     * The account id of the user
     */
    public static final String GLOBAL_PROP_ACCOUNT_ID = "Account Id";

    //endregion


    //region Errors

    /**
     * Anytime an error is encountered, even if it came from server.  MAKE SURE you don't log Error in a loop ... I've seen it happen where 10,000 events get logged :)
     */
    public static final String EVENT_ERROR = "Error";
    public static final String PROP_ERROR_CODE = "code";
    public static final String PROP_ERROR_MESSAGE = "message";

    //endregion


    //region Onboarding

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
     * User lands on Location screen (do not log if user comes from Settings)
     */
    public static final String EVENT_ONBOARDING_LOCATION = "Onboarding Location";

    /**
     * User lands on Enhanced Audio screen
     */
    public static final String EVENT_ONBOARDING_SENSE_AUDIO = "Onboarding Sense Audio";

    /**
     * When user lands on the No BLE screen
     */
    public static final String EVENT_ONBOARDING_NO_BLE = "Onboarding No BLE";

    /**
     * When user lands on the pairing mode help screen (not glowing purple)
     */
    public static final String EVENT_ONBOARDING_PAIRING_MODE_HELP = "Onboarding Pairing Mode Help";

    /**
     * When user lands on the Setting up Sense screen
     */
    public static final String EVENT_ONBOARDING_SENSE_SETUP = "Onboarding Sense Setup";

    /**
     * When user encounters an error during Sense Pairing and is asked whether he/she is setting up second pill or first
     */
    public static final String EVENT_ONBOARDING_SECOND_PILL_CHECK = "Onboarding Second Pill Check";

    /**
     * When user lands on the "Pair your Sense" screen
     */
    public static final String EVENT_ONBOARDING_PAIR_SENSE = "Onboarding Pair Sense";

    /**
     * When user lands on the screen to scan for wifi
     */
    public static final String EVENT_ONBOARDING_WIFI = "Onboarding WiFi";

    /**
     * When the user explicitly rescans for wifi networks.
     */
    public static final String EVENT_ONBOARDING_WIFI_SCAN = "Onboarding WiFi Scan";

    /**
     * When the user lands on the "Enter Wifi Password" screen
     */
    public static final String EVENT_ONBOARDING_WIFI_PASSWORD = "Onboarding WiFi Password";

    /**
     * When user lands on the "Pairing your Sleep Pill" screen
     */
    public static final String EVENT_ONBOARDING_PAIR_PILL = "Onboarding Pair Pill";

    /**
     * When user lands on screen where it asks user to place the pill on the pillow
     */
    public static final String EVENT_ONBOARDING_PILL_PLACEMENT = "Onboarding Pill Placement";

    /**
     * User lands on screen which asks whether they want to pair another pill.
     */
    public static final String EVENT_ONBOARDING_ANOTHER_PILL = "Onboarding Another Pill";

    /**
     * When user lands on the screen that tells them "To connect a second Sleep Pill, Sense needs to be put into pairing mode"
     */
    public static final String EVENT_ONBOARDING_PAIRING_MODE_OFF = "Onboarding Pairing Mode Off";

    /**
     * When user lands on screen that tells partner to get app from hello.is/app
     */
    public static final String EVENT_ONBOARDING_GET_APP = "Onboarding Get App";

    /**
     * When user lands on the screen that explains what the colors of Sense mean.  also known as 'before you sleep"
     */
    public static final String EVENT_ONBOARDING_SENSE_COLORS = "Onboarding Sense Colors";

    /**
     * When user is shown the Room Check screen
     */
    public static final String EVENT_ONBOARDING_ROOM_CHECK = "Onboarding Room Check";

    /**
     * When user is asked to set up their smart alarm during onboarding
     */
    public static final String EVENT_ONBOARDING_FIRST_ALARM = "Onboarding First Alarm";

    /**
     * When user lands on the last onboarding Screen
     */
    public static final String EVENT_ONBOARDING_END = "Onboarding End";

    //endregion


    //region In App

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

    //endregion


    //region Widgets

    /**
     * When the user creates their first home screen widget of a certain type.
     */
    public static final String EVENT_WIDGET_CREATED = "Widget Created";

    /**
     * When the user deletes a home screen widget.
     */
    public static final String EVENT_WIDGET_DELETED = "Widget Deleted";

    public static final String PROP_WIDGET_NAME = "widget name";

    //endregion


    //region Lifecycle

    public static void initialize(@NonNull Context context, @NonNull String apiKey) {
        Analytics.provider = MixpanelAPI.getInstance(context, apiKey);
        Analytics.preferences = context.getSharedPreferences(Constants.INTERNAL_PREFS, 0);
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

    public static void setUserId(@NonNull String userId) {
        if (preferences != null && provider != null) {
            String existingUserId = preferences.getString(Constants.INTERNAL_PREF_ANALYTICS_USER_ID, null);
            provider.getPeople().identify(userId);
            if (existingUserId == null) {
                Logger.info(LOG_TAG, "Identifying user.");
                provider.identify(userId);
            } else if (!existingUserId.equals(userId)) {
                Logger.info(LOG_TAG, "Establishing user alias.");
                try {
                    provider.alias(userId, existingUserId);
                } catch (NullPointerException e) {
                    Logger.error(LOG_TAG, "Mixpanel API is still broken.", e);
                }
            }

            if (Crashlytics.getInstance().isInitialized()) {
                Crashlytics.setUserIdentifier(userId);
            }

            preferences.edit()
                    .putString(Constants.INTERNAL_PREF_ANALYTICS_USER_ID, userId)
                    .apply();
        }
    }

    public static void trackUserSignUp(@Nullable String accountId, @Nullable String name, @NonNull DateTime created) {
        Logger.info(LOG_TAG, "Tracking user sign up { accountId: '" + accountId + "', name: '" + name + "', created: '" + created + "' }");

        if (provider != null) {
            if (accountId == null) {
                accountId = "";
            }

            if (name == null) {
                name = "";
            }

            provider.getPeople().set("$name", name);
            provider.getPeople().set("$created", created.toString());
            provider.getPeople().set(GLOBAL_PROP_ACCOUNT_ID, accountId);
            provider.getPeople().set(GLOBAL_PROP_PLATFORM, PLATFORM);

            provider.registerSuperProperties(createProperties(
                    GLOBAL_PROP_NAME, name,
                    GLOBAL_PROP_PLATFORM, PLATFORM
            ));
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

        Logger.info(LOG_TAG, event + ": " + properties);
    }

    public static void trackError(@NonNull String message, int code) {
        trackEvent(EVENT_ERROR, createProperties(PROP_ERROR_MESSAGE, message, PROP_ERROR_CODE, code));
    }

    //endregion
}
