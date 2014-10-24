package is.hello.sense.util;

public final class Constants {
    public static final String INTERNAL_PREFS = "internal_prefs";
    public static final String INTERNAL_PREF_API_ENV_NAME = "api_env_name";

    public static final String NOTIFICATION_PREFS = "notification_prefs";
    public static final String NOTIFICATION_PREF_REGISTRATION_ID = "registration_id";
    public static final String NOTIFICATION_PREF_APP_VERSION = "app_version_code";

    /**
     * The point at which a gesture's velocity dictates that
     * it be completed regardless of relative position.
     */
    public static final float OPEN_VELOCITY_THRESHOLD = 350f;

    public static final int BLE_DEFAULT_TIMEOUT_MS = 20000;
    public static final int BLE_SCAN_TIMEOUT_MS = 10000;
    public static final int BLE_SET_WIFI_TIMEOUT_MS = 60000; // firmware suggestion

    public static final int ONBOARDING_CHECKPOINT_NONE = 0;
    public static final int ONBOARDING_CHECKPOINT_ACCOUNT = 1;
    public static final int ONBOARDING_CHECKPOINT_QUESTIONS = 2;
    public static final int ONBOARDING_CHECKPOINT_SENSE = 3;
    public static final int ONBOARDING_CHECKPOINT_PILL = 4;
}
