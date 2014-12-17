package is.hello.sense.util;

public final class Constants {
    public static final String INTERNAL_PREFS = "internal_prefs";
    public static final String INTERNAL_PREF_API_ENV_NAME = "api_env_name";
    public static final String INTERNAL_PREF_ANALYTICS_USER_ID = "analytics_user_id";

    public static final String NOTIFICATION_PREFS = "notification_prefs";
    public static final String NOTIFICATION_PREF_REGISTRATION_ID = "registration_id";
    public static final String NOTIFICATION_PREF_APP_VERSION = "app_version_code";

    /**
     * The point at which a gesture's velocity dictates that
     * it be completed regardless of relative position.
     */
    public static final float OPEN_VELOCITY_THRESHOLD = 350f;

    public static final int ONBOARDING_CHECKPOINT_NONE = 0;
    public static final int ONBOARDING_CHECKPOINT_ACCOUNT = 1;
    public static final int ONBOARDING_CHECKPOINT_QUESTIONS = 2;
    public static final int ONBOARDING_CHECKPOINT_SENSE = 3;
    public static final int ONBOARDING_CHECKPOINT_PILL = 4;


    // From Retrofit
    public static final int HTTP_CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s
    public static final int HTTP_READ_TIMEOUT_MILLIS = 20 * 1000; // 20s
    public static final String HTTP_CACHE_NAME = "is.hello.sense.okhttp.cache";
    public static final int HTTP_CACHE_SIZE = 2024 * 10;
}
