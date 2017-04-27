package is.hello.sense.api.model.v2.alerts;

import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;

/**
 * Categories for Alerts
 */
public enum Category implements Enums.FromString {
    EXPANSION_UNREACHABLE,
    SENSE_MUTED,
    REVIEW_ACCOUNTS_PAIRED_TO_SENSE,
    SENSE_NOT_PAIRED,
    SENSE_NOT_SEEN,
    SLEEP_PILL_NOT_PAIRED,
    SLEEP_PILL_NOT_SEEN,
    UNKNOWN;

    public static Category fromString(@Nullable final String category) {
        return Enums.fromString(category, values(), UNKNOWN);
    }
}