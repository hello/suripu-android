package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Seconds;
import org.joda.time.Weeks;
import org.joda.time.Years;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;

@Singleton public class DateFormatter {
    private final Context context;

    @Inject public DateFormatter(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }


    //region Date Format Order

    public static char[] getDateFormatOrder(@NonNull Context context) {
        // Works around <https://code.google.com/p/android/issues/detail?id=82144>
        try {
            return DateFormat.getDateFormatOrder(context);
        } catch (IllegalArgumentException e) {
            Logger.warn(DateFormatter.class.getSimpleName(), "Device's DateFormat#getDateFormatOrder(Context) is faulty", e);
            return new char[] {'y', 'M', 'd'};
        }
    }

    //endregion

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

    public @NonNull String formatAsLocalizedDate(@Nullable LocalDate date) {
        if (date != null) {
            return DateFormat.getDateFormat(context).format(date.toDate());
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

    /**
     * Based on Joda-Android's DateUtils.getRelativeTimeSpanString,
     * except supporting a few more time interval types, and omitting
     * support for dates in the future.
     */
    public @NonNull String formatAsRelativeTime(@Nullable DateTime time) {
        if (time != null) {
            DateTime now = DateTime.now(time.getZone()).withMillisOfSecond(0);
            DateTime roundTime = time.withMillisOfSecond(0);
            if (now.isBefore(roundTime)) {
                Logger.warn(getClass().getSimpleName(), "formatAsRelativeTime not meant to be used with dates in the past");
                return formatAsLocalizedDate(roundTime.toLocalDate());
            }

            Interval interval = new Interval(roundTime, now);

            int pluralRes;
            int count;
            if (Minutes.minutesIn(interval).isLessThan(Minutes.ONE)) {
                count = Seconds.secondsIn(interval).getSeconds();
                pluralRes = R.plurals.format_relative_time_seconds;
            } else if (Hours.hoursIn(interval).isLessThan(Hours.ONE)) {
                count = Minutes.minutesIn(interval).getMinutes();
                pluralRes = R.plurals.format_relative_time_minutes;
            } else if (Days.daysIn(interval).isLessThan(Days.ONE)) {
                count = Hours.hoursIn(interval).getHours();
                pluralRes = R.plurals.format_relative_time_hours;
            } else if (Weeks.weeksIn(interval).isLessThan(Weeks.ONE)) {
                count = Days.daysIn(interval).getDays();
                pluralRes = R.plurals.format_relative_time_days;
            } else if (Months.monthsIn(interval).isLessThan(Months.ONE)) {
                count = Weeks.weeksIn(interval).getWeeks();
                pluralRes = R.plurals.format_relative_time_weeks;
            } else if (Years.yearsIn(interval).isLessThan(Years.ONE)) {
                count = Months.monthsIn(interval).getMonths();
                pluralRes = R.plurals.format_relative_time_months;
            } else {
                return formatAsLocalizedDate(roundTime.toLocalDate());
            }

            return context.getResources().getQuantityString(pluralRes, count, count);
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
    }

    //endregion
}
