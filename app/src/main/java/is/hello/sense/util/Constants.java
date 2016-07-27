package is.hello.sense.util;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public final class Constants {
    public static final String UPDATE_URL = "https://hello.is/app";

    public static final String PERSISTENT_PREFS = "persistent_prefs";

    public static final String INTERNAL_PREFS = "internal_prefs";
    public static final String INTERNAL_PREF_BACKSIDE_CURRENT_ITEM = "underside_current_item";
    public static final String INTERNAL_PREF_BACKSIDE_CURRENT_ITEM_LAST_UPDATED = "underside_current_item_last_updated";
    public static final String INTERNAL_PREF_ACCOUNT_ID = "INTERNAL_PREF_ACCOUNT_ID";

    public static final String NOTIFICATION_PREFS = "notification_prefs";
    public static final String NOTIFICATION_PREF_REGISTRATION_ID = "registration_id";
    public static final String NOTIFICATION_PREF_APP_VERSION = "app_version_code";

    public static final String HANDHOLDING_PREFS = "handholding_prefs";
    public static final String HANDHOLDING_HAS_SHOWN_TIMELINE_ADJUST_INTRO = "has_shown_timeline_adjust_intro";
    public static final String HANDHOLDING_NUMBER_TIMELINES_SHOWN = "number_timelines_shown";

    public static final String SLEEP_SOUNDS_PREFS = "sleep_sounds_prefs";
    public static final String SLEEP_SOUNDS_SOUND_ID = "sleep_sounds_sound_id";
    public static final String SLEEP_SOUNDS_DURATION_ID = "sleep_sounds_duration_id";
    public static final String SLEEP_SOUNDS_VOLUME_ID = "sleep_sounds_volume_id";

    public static final String WHATS_NEW_LAYOUT_SHOULD_SHOW_PREFS = "WHATS_NEW_LAYOUT_SHOULD_SHOW_PREFS";
    public static final String WHATS_NEW_LAYOUT_LAST_VERSION_SHOWN = "WHATS_NEW_LAYOUT_LAST_VERSION_SHOWN";
    public static final String WHATS_NEW_LAYOUT_TIMES_SHOWN = "WHATS_NEW_LAYOUT_TIMES_SHOWN";
    public static final String WHATS_NEW_LAYOUT_FORCE_SHOW = "WHATS_NEW_LAYOUT_FORCE_SHOW";

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
    public static final int ONBOARDING_CHECKPOINT_SMART_ALARM = 5;

    public static final int ONBOARDING_CHECKPOINT_SENSE_VOICE = 7;

    public static final long STALE_INTERVAL_MS = (10 * 60 * 1000); // 10 minutes

    /**
     * The oldest date that we have timeline data for.
     * <p>
     * The selected date is intentionally overly optimistic. Actual
     * production data starts on <code>2015-02-14 21:28:00</code>.
     */
    public static final LocalDate TIMELINE_EPOCH = new LocalDate(2014, 1, 1);

    public static final LocalDate RELEASE_DATE_FOR_LAST_NAME = new DateTime()
            .withYear(2016)
            .withMonthOfYear(6)
            .withDayOfMonth(15)
            .toLocalDate();


    // From Retrofit
    public static final int HTTP_CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s
    public static final int HTTP_READ_TIMEOUT_MILLIS = 20 * 1000; // 20s
    public static final String HTTP_CACHE_NAME = "is.hello.sense.okhttp.cache";
    public static final int HTTP_CACHE_SIZE = 2024 * 10;
}
