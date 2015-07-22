package is.hello.sense.notifications;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public enum Notification {
    TIMELINE,
    SENSOR,
    TRENDS,
    ALARM,
    SETTINGS,
    INSIGHTS;

    //region Creation

    public static Notification fromBundle(@NonNull Bundle notifications) {
        return fromString(notifications.getString(NotificationReceiver.EXTRA_TARGET));
    }

    public static Notification fromString(@Nullable String string) {
        return Enums.fromString(string, values(), TIMELINE);
    }
    //region


    //region Accessors

    public static @NonNull LocalDate getDate(@NonNull Bundle notification) {
        String rawDate = notification.getString(NotificationReceiver.EXTRA_DETAILS);
        if (TextUtils.isEmpty(rawDate)) {
            return DateFormatter.lastNight();
        }

        try {
            DateTimeFormatter formatter = DateTimeFormat.forPattern(ApiService.DATE_FORMAT);
            return formatter.parseLocalDate(rawDate);
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            Logger.error(Notification.class.getSimpleName(), "Could not parse timestamp from timeline notification", e);
            return DateFormatter.lastNight();
        }
    }

    public static @NonNull String getSensorName(@NonNull Bundle notification) {
        return notification.getString(NotificationReceiver.EXTRA_DETAILS, ApiService.SENSOR_NAME_TEMPERATURE);
    }

    //endregion
}
