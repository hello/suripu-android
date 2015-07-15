package is.hello.sense.api.model;

import android.support.annotation.NonNull;

public enum InsightCategory implements Enums.FromString {
    GENERIC,
    SLEEP_HYGIENE,
    LIGHT,
    SOUND,
    TEMPERATURE,
    HUMIDITY,
    AIR_QUALITY,
    SLEEP_DURATION,
    TIME_TO_SLEEP,
    SLEEP_TIME,
    WAKE_TIME,
    WORKOUT,
    CAFFEINE,
    ALCOHOL,
    DIET,
    DAYTIME_SLEEPINESS,
    DAYTIME_ACTIVITIES,
    SLEEP_SCORE,
    SLEEP_QUALITY,
    IN_APP_ERROR;

    public static InsightCategory fromString(@NonNull String value) {
            return Enums.fromString(value, values(), GENERIC);
    }
}
