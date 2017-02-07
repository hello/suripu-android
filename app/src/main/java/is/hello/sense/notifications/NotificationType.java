package is.hello.sense.notifications;

import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;

public enum NotificationType {
    SLEEP_SCORE,
    PILL_BATTERY;

    //region Creation

    public static NotificationType fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), SLEEP_SCORE);
    }
    //region
}
