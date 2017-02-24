package is.hello.sense.notifications;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class Notification extends ApiResponse {
    static final String REMOTE_TITLE = "hlo_title";
    static final String REMOTE_BODY = "hlo_body";
    /**
     * Member of {@link is.hello.sense.notifications.Notification.Type}
     */
    static final String REMOTE_TYPE = "hlo_type";
    /**
     * Key to fetch string value that will be empty string
     * if not used.
     * ex. hlo_detail = 2017-12-31
     * if hlo_type = {@link is.hello.sense.notifications.Notification.Type#SLEEP_SCORE}
     */
    static final String REMOTE_DETAIL = "hlo_detail";

    static final String EXTRA_TYPE = "extra_type";
    static final String EXTRA_DETAILS = "extra_details";

    @NonNull
    public static Notification fromBundle(@NonNull final Bundle bundle) {
        return new Notification(typeFromBundle(bundle), bundle.getString(EXTRA_DETAILS, UNKNOWN));
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({SYSTEM, SLEEP_SCORE, UNKNOWN})
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.FIELD})
    public @interface Type {
    }

    public static final String SYSTEM = "SYSTEM";
    public static final String SLEEP_SCORE = "SLEEP_SCORE";
    public static final String UNKNOWN = "UNKNOWN";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({PILL_BATTERY, UNKNOWN})
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    public @interface SystemType {
    }

    public static final String PILL_BATTERY = "PILL_BATTERY";

    @NonNull
    @Type
    public static String typeFromBundle(@NonNull final Bundle notifications) {
        return typeFromString(notifications.getString(EXTRA_TYPE));
    }

    @NonNull
    @Type
    public static String typeFromString(@Nullable final String string) {
        if(string == null) {
            return UNKNOWN;
        }
        switch (string.toUpperCase(Locale.ENGLISH)) {
            case SYSTEM: return SYSTEM;
            case SLEEP_SCORE: return SLEEP_SCORE;
            default: return UNKNOWN;
        }
    }

    @NonNull
    @SystemType
    public static String systemTypeFromBundle(@NonNull final Bundle notifications) {
        if(SYSTEM.equals(typeFromBundle(notifications))) {
            return systemTypeFromString(notifications.getString(EXTRA_DETAILS));
        } else {
            return UNKNOWN;
        }
    }

    @NonNull
    @SystemType
    public static String systemTypeFromString(@Nullable final String string) {
        if(string == null) {
            return UNKNOWN;
        }
        switch (string.toUpperCase(Locale.ENGLISH)) {
            case PILL_BATTERY: return PILL_BATTERY;
            default: return UNKNOWN;
        }
    }

    @NonNull
    @Type
    private final String type;

    @Nullable
    private final String detail;

    private boolean seen;

    public Notification(@NonNull @Type final String type,
                        @Nullable final String detail) {
        this.type = type;
        this.detail = detail;
        this.seen = false;
    }

    @NonNull
    @Type
    public String getType() {
        return type;
    }

    @Nullable
    public String getDetail() {
        return detail;
    }

    void setSeen(final boolean seen) {
        this.seen = seen;
    }

    boolean hasSeen() {
        return seen;
    }

    @NonNull
    public LocalDate getDate() {
        final String rawDate = detail;
        if (TextUtils.isEmpty(rawDate)) {
            return DateFormatter.lastNight();
        }

        try {
            final DateTimeFormatter formatter = DateTimeFormat.forPattern(ApiService.DATE_FORMAT);
            return formatter.parseLocalDate(rawDate);
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            Logger.error(Notification.class.getSimpleName(), "Could not parse timestamp from timeline notification", e);
            return DateFormatter.lastNight();
        }
    }
}
