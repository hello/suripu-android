package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum InsightCategory {
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
    SLEEP_QUALITY;

    @JsonCreator
    @SuppressWarnings("UnusedDeclaration")
    public static InsightCategory fromString(@NonNull String value) {
            return Enums.fromString(value, values(), GENERIC);
    }
}
