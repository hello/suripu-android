package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.PreferencesPresenter;

@Singleton public class DateFormatter {
    private final Context context;
    private final PreferencesPresenter preferences;

    @Inject public DateFormatter(@NonNull Context context,
                                 @NonNull PreferencesPresenter preferences) {
        this.context = context.getApplicationContext();
        this.preferences = preferences;
    }


    //region Last Night

    public static boolean isLastNight(@NonNull DateTime instant) {
        Interval interval = new Interval(Days.ONE, DateTime.now().withTimeAtStartOfDay());
        return interval.contains(instant);
    }

    public static @NonNull DateTime lastNight() {
        return DateTime.now().minusDays(1);
    }

    //endregion


    //region Primitive Formatters

    public @NonNull DateTimeZone getSenseTimeZone() {
        String pairedDeviceTimeZone = preferences.getString(PreferencesPresenter.PAIRED_DEVICE_TIME_ZONE, null);
        if (TextUtils.isEmpty(pairedDeviceTimeZone)) {
            return DateTimeZone.getDefault();
        } else {
            return DateTimeZone.forID(pairedDeviceTimeZone);
        }
    }

    /**
     * Formats a given DateTime instance according to a given pattern, applying the formatter's target time zone.
     */
    public @NonNull String formatDateTime(@NonNull DateTime dateTime, @NonNull String pattern) {
        return dateTime.toString(pattern);
    }

    //endregion


    //region Core Formatters

    public @NonNull String formatAsTimelineDate(@Nullable DateTime date) {
        if (date != null && isLastNight(date))
            return context.getString(R.string.format_date_last_night);
        else
            return formatAsDate(date);
    }

    public @NonNull String formatAsBirthDate(@Nullable LocalDate date) {
        if (date != null) {
            return date.toString(context.getString(R.string.format_birth_date));
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
    }

    public @NonNull String formatAsDate(@Nullable DateTime date) {
        if (date != null) {
            return formatDateTime(date, context.getString(R.string.format_date));
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
    }

    public @NonNull String formatAsTimelineStamp(@Nullable DateTime date, boolean use24Time) {
        if (date != null) {
            if (use24Time)
                return date.toString(context.getString(R.string.format_timeline_time_24_hr));
            else
                return date.toString(context.getString(R.string.format_timeline_time_12_hr));
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @NonNull String formatAsTime(@Nullable LocalDateTime date, boolean use24Time) {
        if (date != null) {
            if (use24Time)
                return date.toString(context.getString(R.string.format_time_24_hr));
            else
                return date.toString(context.getString(R.string.format_time_12_hr));
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @NonNull String formatAsTime(@Nullable LocalTime time, boolean use24Time) {
        if (time != null) {
            if (use24Time)
                return time.toString(context.getString(R.string.format_time_24_hr));
            else
                return time.toString(context.getString(R.string.format_time_12_hr));
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @NonNull String formatAsTime(@Nullable DateTime time, boolean use24Time) {
        if (time != null) {
            if (use24Time) {
                return time.toString(context.getString(R.string.format_time_24_hr));
            } else {
                return time.toString(context.getString(R.string.format_time_12_hr));
            }
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @NonNull String formatAsDayAndTime(@Nullable DateTime time, boolean use24Time) {
        if (time != null) {
            if (use24Time) {
                return time.toString(context.getString(R.string.format_day_and_time_24_hr));
            } else {
                return time.toString(context.getString(R.string.format_day_and_time_12_hr));
            }
        }
        return context.getString(R.string.format_date_placeholder);
    }

    //endregion
}
