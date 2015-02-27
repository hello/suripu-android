package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.danlew.android.joda.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Weeks;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;

@Singleton public class DateFormatter {
    private final Context context;

    @Inject public DateFormatter(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }


    //region Last Night

    /**
     * Returns a DateTime representing the current instant in time,
     * shifted into the user's local device time zone.
     */
    public static @NonNull DateTime now() {
        return DateTime.now(DateTimeZone.getDefault());
    }

    /**
     * Returns the date considered to represent last night.
     */
    public static @NonNull DateTime lastNight() {
        return now().minusDays(1);
    }

    /**
     * Returns whether or not a given date is considered to be last night.
     */
    public static boolean isLastNight(@NonNull DateTime instant) {
        Interval interval = new Interval(Days.ONE, now().withTimeAtStartOfDay());
        return interval.contains(instant);
    }

    /**
     * Returns whether or not a given date is considered to
     * be within the week before the present day.
     */
    public static boolean isInLastWeek(@NonNull DateTime instant) {
        Interval interval = new Interval(Weeks.ONE, now().withTimeAtStartOfDay());
        return interval.contains(instant);
    }

    //endregion


    //region Core Formatters

    public @NonNull String formatAsTimelineDate(@Nullable DateTime date) {
        if (date != null) {
            if (isLastNight(date)) {
                return context.getString(R.string.format_date_last_night);
            } else if (isInLastWeek(date)) {
                return date.toString(context.getString(R.string.format_date_weekday));
            } else {
                return date.toString(context.getString(R.string.format_date));
            }
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
    }

    public @NonNull String formatAsBirthDate(@Nullable LocalDate date) {
        if (date != null) {
            return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE);
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
