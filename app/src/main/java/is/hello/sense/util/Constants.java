package is.hello.sense.util;

public final class Constants {
    public static final String UPDATE_URL = "https://hello.is/app";

    public static final String INTERNAL_PREFS = "internal_prefs";
    public static final String INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM = "underside_current_item";
    public static final String INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM_LAST_UPDATED = "underside_current_item_last_updated";

    public static final String NOTIFICATION_PREFS = "notification_prefs";
    public static final String NOTIFICATION_PREF_REGISTRATION_ID = "registration_id";
    public static final String NOTIFICATION_PREF_APP_VERSION = "app_version_code";

    public static final String HANDHOLDING_PREFS = "handholding_prefs";
    public static final String HANDHOLDING_HAS_SHOWN_TIMELINE_ADJUST_INTRO = "has_shown_timeline_adjust_intro";
    public static final String HANDHOLDING_NUMBER_TIMELINES_SHOWN = "number_timelines_shown";

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

    public static final long STALE_INTERVAL_MS = (10 * 60 * 1000); // 10 minutes

    // From Retrofit
    public static final int HTTP_CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s
    public static final int HTTP_READ_TIMEOUT_MILLIS = 20 * 1000; // 20s
    public static final String HTTP_CACHE_NAME = "is.hello.sense.okhttp.cache";
    public static final int HTTP_CACHE_SIZE = 2024 * 10;
}
