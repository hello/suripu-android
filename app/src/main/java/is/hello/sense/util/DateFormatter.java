package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;

@Singleton public class DateFormatter {
    /**
     * The hour of day when the last night date is considered to roll over.
     * <p />
     * <code>3:00 AM</code> chosen to support early first shift risers.
     */
    public static final int NIGHT_BOUNDARY_HOUR = 3;

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
    @VisibleForTesting
    static @NonNull DateTime nowDateTime() {
        return DateTime.now(DateTimeZone.getDefault());
    }

    /**
     * Returns a LocalDate representing the current instant,
     * shifted into the user's local device time zone.
     */
    @VisibleForTesting
    static @NonNull LocalDate nowLocalDate() {
        return LocalDate.now(DateTimeZone.getDefault());
    }

    /**
     * Returns the date considered to represent today for the timeline.
     * <p>
     * Should not be used for anything thing that does not require
     * the {@link #NIGHT_BOUNDARY_HOUR} constant to apply.
     */
    public static @NonNull LocalDate todayForTimeline() {
        final DateTime now = nowDateTime();
        if (now.getHourOfDay() < NIGHT_BOUNDARY_HOUR) {
            return now.minusDays(1).toLocalDate();
        } else {
            return now.toLocalDate();
        }
    }

    /**
     * Returns the date considered to represent last night.
     */
    public static @NonNull LocalDate lastNight() {
        final DateTime now = nowDateTime();
        if (now.getHourOfDay() < NIGHT_BOUNDARY_HOUR) {
            return now.minusDays(2).toLocalDate();
        } else {
            return now.minusDays(1).toLocalDate();
        }
    }

    /**
     * Returns whether or not a given date is considered to be last night.
     */
    public static boolean isLastNight(@NonNull LocalDate instant) {
        return lastNight().isEqual(instant);
    }

    /**
     * Returns whether or not a given date is considered to
     * be within the week before the present day.
     */
    public static boolean isInLastWeek(@NonNull LocalDate instant) {
        Interval interval = new Interval(Weeks.ONE, nowDateTime().withTimeAtStartOfDay());
        return interval.contains(instant.toDateTimeAtStartOfDay());
    }

    //endregion


    //region Core Formatters

    public @NonNull String formatAsTimelineDate(@Nullable LocalDate date) {
        if (date != null) {
            final LocalDate lastNight = lastNight();
            if (date.equals(lastNight) || date.isAfter(lastNight)) {
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

    public @NonNull String formatAsTimelineNavigatorDate(@Nullable LocalDate date) {
        if (date != null) {
            if (!date.year().equals(nowLocalDate().year())) {
                return date.toString(context.getString(R.string.format_date_month_year));
            } else {
                return date.toString(context.getString(R.string.format_date_month));
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

    public static @NonNull CharSequence assembleTimeAndPeriod(@NonNull CharSequence time, @Nullable CharSequence period) {
        if (TextUtils.isEmpty(period)) {
            return time;
        } else {
            SpannableStringBuilder spannable = new SpannableStringBuilder(period);
            spannable.setSpan(new RelativeSizeSpan(0.75f), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.insert(0, time);
            return spannable;
        }
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

    public @Nullable CharSequence formatForTimelineSegment(@Nullable LocalTime date, boolean use24Time) {
        if (date != null) {
            String hour, period;
            if (use24Time) {
                hour = date.toString(context.getString(R.string.format_timeline_segment_time_24_hr));
                period = "";
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

    @NotLocalizable(NotLocalizable.BecauseOf.API_LIMITATION)
    public @NonNull CharSequence formatAsAlarmTime(@Nullable LocalTime time, boolean use24Time) {
        if (time != null) {
            if (use24Time) {
                return time.toString(context.getString(R.string.format_alarm_time_24_hr));
            } else {
                final String period = time.toString(context.getString(R.string.format_alarm_time_12_hr_period_padded));
                final SpannableStringBuilder rendered = new SpannableStringBuilder(period);
                rendered.setSpan(new RelativeSizeSpan(0.4f),
                                 0, rendered.length(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                rendered.setSpan(new TypefaceSpan("sans-serif-light"),
                                 0, rendered.length(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                final String timeString = time.toString(context.getString(R.string.format_alarm_time_12_hr));
                rendered.insert(0, timeString);

                return rendered;
            }
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
            return Styles.assembleReadingAndUnit(Long.toString(totalMinutes), context.getString(R.string.format_duration_abbrev_minutes), Styles.UNIT_STYLE_SUBSCRIPT);
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


    //region Calendar Support

    public static @JodaWeekDay int calendarDayToJodaTimeDay(int calendarDay) {
        switch (calendarDay) {
            case Calendar.SUNDAY: {
                return DateTimeConstants.SUNDAY;
            }
            case Calendar.MONDAY: {
                return DateTimeConstants.MONDAY;
            }
            case Calendar.TUESDAY: {
                return DateTimeConstants.TUESDAY;
            }
            case Calendar.WEDNESDAY: {
                return DateTimeConstants.WEDNESDAY;
            }
            case Calendar.THURSDAY: {
                return DateTimeConstants.THURSDAY;
            }
            case Calendar.FRIDAY: {
                return DateTimeConstants.FRIDAY;
            }
            case Calendar.SATURDAY: {
                return DateTimeConstants.SATURDAY;
            }
            default: {
                Logger.warn(DateFormatter.class.getSimpleName(),
                            "Unknown calendar day '" + calendarDay + "', defaulting to MONDAY.");
                return DateTimeConstants.MONDAY;
            }
        }
    }

    public static int jodaTimeDayToCalendarDay(@JodaWeekDay int dateTimeDay) {
        switch (dateTimeDay) {
            case DateTimeConstants.SUNDAY: {
                return Calendar.SUNDAY;
            }
            case DateTimeConstants.MONDAY: {
                return Calendar.MONDAY;
            }
            case DateTimeConstants.TUESDAY: {
                return Calendar.TUESDAY;
            }
            case DateTimeConstants.WEDNESDAY: {
                return Calendar.WEDNESDAY;
            }
            case DateTimeConstants.THURSDAY: {
                return Calendar.THURSDAY;
            }
            case DateTimeConstants.FRIDAY: {
                return Calendar.FRIDAY;
            }
            case DateTimeConstants.SATURDAY: {
                return Calendar.SATURDAY;
            }
            default: {
                Logger.warn(DateFormatter.class.getSimpleName(),
                            "Unknown calendar day '" + dateTimeDay + "', defaulting to MONDAY.");
                return Calendar.MONDAY;
            }
        }
    }

    public static @JodaWeekDay int nextJodaTimeDay(@JodaWeekDay final int jodaTimeDay) {
        final @JodaWeekDay int nextDay = jodaTimeDay + 1;
        if (nextDay > DateTimeConstants.SUNDAY) {
            return DateTimeConstants.MONDAY;
        } else {
            return nextDay;
        }
    }

    public static List<Integer> getDaysOfWeek(@JodaWeekDay final int startDay) {
        final List<Integer> days = new ArrayList<>(7);
        @JodaWeekDay int day = startDay;
        do {
            days.add(day);
            day = nextJodaTimeDay(day);
        } while (day != startDay);
        return days;
    }

    //endregion


    @IntDef({
            DateTimeConstants.SUNDAY,
            DateTimeConstants.MONDAY,
            DateTimeConstants.TUESDAY,
            DateTimeConstants.WEDNESDAY,
            DateTimeConstants.THURSDAY,
            DateTimeConstants.FRIDAY,
            DateTimeConstants.SATURDAY,
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD,
            ElementType.FIELD, ElementType.LOCAL_VARIABLE})
    public @interface JodaWeekDay {}
}
