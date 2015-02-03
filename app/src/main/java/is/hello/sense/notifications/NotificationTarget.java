package is.hello.sense.notifications;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonCreator;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Enums;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public enum NotificationTarget {
    TIMELINE {
        public @NonNull DateTime getDateTime(@NonNull Bundle notification) {
            String rawDate = notification.getString(NotificationReceiver.EXTRA_DETAILS);
            if (TextUtils.isEmpty(rawDate)) {
                return DateFormatter.lastNight();
            }

            try {
                DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
                return formatter.parseDateTime(rawDate);
            } catch (UnsupportedOperationException | IllegalArgumentException e) {
                Logger.error(getClass().getSimpleName(), "Could not parse timestamp from timeline notification", e);
                return DateFormatter.lastNight();
            }
        }
    },
    SENSOR {
        public @NonNull String getName(@NonNull Bundle notification) {
            return notification.getString(NotificationReceiver.EXTRA_DETAILS, ApiService.SENSOR_NAME_TEMPERATURE);
        }
    },
    TRENDS,
    ALARM,
    SETTINGS,
    INSIGHTS;

    public static NotificationTarget fromNotification(@NonNull Bundle notifications) {
        return fromString(notifications.getString(NotificationReceiver.EXTRA_TARGET));
    }

    @JsonCreator
    public static NotificationTarget fromString(@Nullable String string) {
        return Enums.fromString(string, values(), TIMELINE);
    }
}
