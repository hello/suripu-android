package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amplitude.api.Amplitude;

import org.json.JSONException;
import org.json.JSONObject;

public class Analytics {
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
     * When user lands on Getting Started Screen
     */
    public static final String EVENT_ONBOARDING_SETUP_START = "Onboarding Setup Start";

    /**
     * When user lands on the "First one here?" screen
     */
    public static final String EVENT_ONBOARDING_SETUP_TWO_PILL = "Onboarding Setup Two Pill";

    /**
     * When user lands on the "Add a second Sleep Pill" screen
     */
    public static final String EVENT_ONBOARDING_ADD_PILL = "Onboarding Add Pill";

    /**
     * When user lands on the Setting up Sense screen
     */
    public static final String EVENT_ONBOARDING_SENSE_SETUP = "Onboarding Sense Setup";

    /**
     * When user lands on the "Pair your Sense" screen
     */
    public static final String EVENT_ONBOARDING_PAIR_SENSE = "Onboarding Pair Sense";

    /**
     * When user lands on the "Connecting Sense to WiFi" screen
     */
    public static final String EVENT_ONBOARDING_SETUP_WIFI = "Onboarding Setup WiFi";

    /**
     * When user lands on the "Introducing Sleep Pill" screen
     */
    public static final String EVENT_ONBOARDING_SETUP_PILL = "Onboarding Setup Pill";

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


    public static @NonNull JSONObject createProperties(@NonNull Object... pairs) {
        if ((pairs.length % 2) != 0)
            throw new IllegalArgumentException("even number of arguments required");

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
        if (properties != null)
            Amplitude.logEvent(event, properties);
        else
            Amplitude.logEvent(event);
    }

    public static void error(@NonNull String message, int code) {
        event(EVENT_ERROR, createProperties(PROP_ERROR_MESSAGE, message,
                                            PROP_ERROR_CODE, code));
    }
}
