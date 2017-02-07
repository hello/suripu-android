package is.hello.sense.notifications;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

import static is.hello.sense.notifications.NotificationType.fromString;

public class Notification extends ApiResponse {
    static final String REMOTE_TITLE = "hlo_title";
    static final String REMOTE_BODY = "hlo_body";
    /**
     * Member of {@link NotificationType}
     */
    static final String REMOTE_TYPE = "hlo_type";
    /**
     * Key to fetch string value that will be empty string
     * if not used.
     * ex. hlo_detail = 2017-12-31
     * if hlo_type = {@link NotificationType#SLEEP_SCORE}
     */
    static final String REMOTE_DETAIL = "hlo_detail";

    static final String EXTRA_TYPE = "extra_type";
    static final String EXTRA_DETAILS = "extra_details";

    public static NotificationType typeFromBundle(@NonNull final Bundle notifications) {
        return fromString(notifications.getString(EXTRA_TYPE));
    }

    public static @NonNull
    LocalDate getDate(@NonNull final Bundle notification) {
        final String rawDate = notification.getString(EXTRA_DETAILS);
        if (TextUtils.isEmpty(rawDate)) {
            return DateFormatter.lastNight();
        }

        try {
            final DateTimeFormatter formatter = DateTimeFormat.forPattern(ApiService.DATE_FORMAT);
            return formatter.parseLocalDate(rawDate);
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            Logger.error(NotificationType.class.getSimpleName(), "Could not parse timestamp from timeline notification", e);
            return DateFormatter.lastNight();
        }
    }
}
