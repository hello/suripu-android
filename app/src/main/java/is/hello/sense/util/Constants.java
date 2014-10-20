package is.hello.sense.util;

public final class Constants {
    public static final String INTERNAL_PREFS = "internal_prefs";
    public static final String INTERNAL_PREF_API_ENV_NAME = "api_env_name";

    public static final String NOTIFICATION_PREFS = "notification_prefs";
    public static final String NOTIFICATION_PREF_REGISTRATION_ID = "registration_id";
    public static final String NOTIFICATION_PREF_APP_VERSION = "app_version";

    public static final String GLOBAL_PREF_UNIT_SYSTEM = "unit_system_name";
    public static final String GLOBAL_PREF_USE_24_TIME = "use_24_time";
    public static final String GLOBAL_PREF_PAIRED_DEVICE_ADDRESS = "paired_device_address";

    /**
     * The point at which a gesture's velocity dictates that
     * it be completed regardless of relative position.
     */
    public static final float OPEN_VELOCITY_THRESHOLD = 350f;

    public static final int BLE_DEFAULT_TIMEOUT_MS = 200000;
    public static final int BLE_SCAN_TIMEOUT_MS = 10000;
    public static final int BLE_SET_WIFI_TIMEOUT_MS = 60000; // firmware suggestion

    public static final String HOCKEY_APP_ID = "805427569ce2035dcda0b99e4d984256";
}
