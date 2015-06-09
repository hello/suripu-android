package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;

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

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

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

    public static @NonNull CharSequence assembleTimeAndPeriod(@NonNull CharSequence time, @NonNull CharSequence period) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(period);
        spannable.setSpan(new RelativeSizeSpan(0.75f), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.insert(0, time);
        return spannable;
    }

    public @NonNull CharSequence formatForTimelineEvent(@Nullable DateTime date, boolean use24Time) {
        if (date != null) {
            if (use24Time) {
                return date.toString(context.getString(R.string.format_timeline_event_time_24_hr));
            } else {
                String time = date.toString(context.getString(R.string.format_timeline_event_time_12_hr));
                String period = date.toString(context.getString(R.string.format_timeline_12_hr_period_padded));
                return assembleTimeAndPeriod(time, period);
            }
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @Nullable CharSequence formatForTimelineSegment(@Nullable DateTime date, boolean use24Time) {
        if (date != null) {
            String hour, period;
            if (use24Time) {
                hour = date.toString(context.getString(R.string.format_timeline_segment_time_24_hr));
                period = context.getString(R.string.format_timeline_24_hr_period);
            } else {
                hour = date.toString(context.getString(R.string.format_timeline_segment_time_12_hr));
                period = date.toString(context.getString(R.string.format_timeline_12_hr_period_padded));
            }

            return assembleTimeAndPeriod(hour, period);
        } else {
            return null;
        }
    }

    public @NonNull CharSequence formatForTimelineInfo(@Nullable DateTime date, boolean use24Time) {
        if (date != null) {
            if (use24Time) {
                return date.toString(context.getString(R.string.format_timeline_event_time_24_hr));
            } else {
                String time = date.toString(context.getString(R.string.format_timeline_event_time_12_hr));
                String period = date.toString(context.getString(R.string.format_timeline_12_hr_period));
                return Styles.assembleReadingAndUnit(time, period, Styles.UNIT_STYLE_SUBSCRIPT);
            }
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

    public @NonNull CharSequence formatDuration(long duration, @NonNull TimeUnit unit) {
        long totalMinutes = unit.toMinutes(duration);
        if (totalMinutes < 60) {
            return Styles.assembleReadingAndUnit(totalMinutes, context.getString(R.string.format_duration_abbrev_minutes));
        } else {
            float hours = totalMinutes / 60f;
            long leftOverMinutes = totalMinutes % 60;

            String reading;
            if (leftOverMinutes == 0) {
                reading = String.format("%.0f", hours);
            } else {
                reading = String.format("%.1f", hours);
            }

            return Styles.assembleReadingAndUnit(reading, context.getString(R.string.format_duration_abbrev_hours), Styles.UNIT_STYLE_SUBSCRIPT);
        }
    }

    //endregion
}
