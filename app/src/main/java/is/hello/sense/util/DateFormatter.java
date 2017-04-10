package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;
import is.hello.sense.interactors.Interactor;
import is.hello.sense.ui.widget.util.Styles;

@Singleton
public class DateFormatter extends Interactor {
    /**
     * The hour of day when the last night date is considered to roll over.
     * <p/>
     * <code>3:00 AM</code> chosen to support early first shift risers.
     */
    public static final int NIGHT_BOUNDARY_HOUR = 3;

    private final Context context;

    @Inject
    public DateFormatter(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }


    //region Date Format Order

    public static char[] getDateFormatOrder(@NonNull Context context) {
        // Works around <https://code.google.com/p/android/issues/detail?id=82144>
        try {
            return DateFormat.getDateFormatOrder(context);
        } catch (IllegalArgumentException e) {
            Logger.warn(DateFormatter.class.getSimpleName(), "Device's DateFormat#getDateFormatOrder(Context) is faulty", e);
            return new char[]{'y', 'M', 'd'};
        }
    }

    //endregion

    //region Last Night

    /**
     * Returns a DateTime representing the current instant in time,
     * shifted into the user's local device time zone.
     */
    @VisibleForTesting
    static
    @NonNull
    DateTime nowDateTime() {
        return DateTime.now(DateTimeZone.getDefault());
    }

    /**
     * Returns a LocalDate representing the current instant,
     * shifted into the user's local device time zone.
     */
    @VisibleForTesting
    static
    @NonNull
    LocalDate nowLocalDate() {
        return LocalDate.now(DateTimeZone.getDefault());
    }

    /**
     * Returns the date considered to represent today for the timeline.
     * <p>
     * Should not be used for anything thing that does not require
     * the {@link #NIGHT_BOUNDARY_HOUR} constant to apply.
     */
    public static
    @NonNull
    LocalDate todayForTimeline() {
        final DateTime now = nowDateTime();
        if (now.getHourOfDay() < NIGHT_BOUNDARY_HOUR) {
            return now.minusDays(1).toLocalDate();
        } else {
            return now.toLocalDate();
        }
    }

    /**
     * Returns whether or not a given local date is considered to be today.
     */
    public static boolean isTodayForTimeline(@Nullable final LocalDate localDate) {
        if (localDate == null) {
            return false;
        }
        return todayForTimeline().equals(localDate);
    }

    /**
     * Returns the date considered to represent last night.
     */
    public static
    @NonNull
    LocalDate lastNight() {
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
    public static boolean isLastNight(@Nullable final LocalDate instant) {
        if (instant == null) {
            return false;
        }
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


    /**
     * Returns whether or not a given date is considered to
     * be within two weeks before the present day.
     */
    public static boolean isInLast2Weeks(@NonNull final LocalDate instant) {
        final Interval interval = new Interval(Weeks.TWO, nowDateTime().withTimeAtStartOfDay());
        return interval.contains(instant.toDateTimeAtStartOfDay());
    }

    public static boolean isMoreThanThreeDays(@NonNull final LocalDate creationDate) {
        final LocalDate today = LocalDate.now();
        return Math.abs(Days.daysBetween(today, creationDate).getDays()) > 3;

    }

    public static boolean isBetween(@NonNull final DateTime instant,
                                    @NonNull final DateTime start,
                                    @NonNull final DateTime end) {
        return new Interval(start, end).contains(instant);

    }

    //endregion

    //region Week Periods

    /**
     * Checks whether a collection of {@code DateTimeConstants} represents the weekdays.
     */
    public static boolean isWeekdays(@NonNull Collection<Integer> days) {
        return (days.size() == 5 &&
                days.contains(DateTimeConstants.MONDAY) &&
                days.contains(DateTimeConstants.TUESDAY) &&
                days.contains(DateTimeConstants.WEDNESDAY) &&
                days.contains(DateTimeConstants.THURSDAY) &&
                days.contains(DateTimeConstants.FRIDAY));
    }

    /**
     * Checks whether a collection of {@code DateTimeConstants} represents the weekend.
     */
    public static boolean isWeekend(@NonNull Collection<Integer> days) {
        return (days.size() == 2 &&
                days.contains(DateTimeConstants.SATURDAY) &&
                days.contains(DateTimeConstants.SUNDAY));
    }

    //endregion


    //region Core Formatters

    public
    @NonNull
    String formatAsTimelineDate(@Nullable LocalDate date) {
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

    public
    @NonNull
    String formatAsTimelineNavigatorDate(@Nullable LocalDate date) {
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

    public
    @NonNull
    String formatAsLocalizedDate(@Nullable LocalDate date) {
        if (date != null) {
            return DateFormat.getDateFormat(context).format(date.toDate());
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
    }

    public static
    @NonNull
    CharSequence assembleTimeAndPeriod(@NonNull CharSequence time, @Nullable CharSequence period) {
        if (TextUtils.isEmpty(period)) {
            return time;
        } else {
            SpannableStringBuilder spannable = new SpannableStringBuilder(period);
            spannable.setSpan(new RelativeSizeSpan(0.75f), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.insert(0, time);
            return spannable;
        }
    }

    public
    @NonNull
    CharSequence formatForTimelineEvent(@Nullable DateTime date, boolean use24Time) {
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

    public
    @Nullable
    CharSequence formatForTimelineSegment(@Nullable LocalTime date, boolean use24Time) {
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

    public
    @NonNull
    CharSequence formatForTimelineInfo(@Nullable DateTime date, boolean use24Time) {
        if (date != null) {
            if (use24Time) {
                return date.toString(context.getString(R.string.format_timeline_event_time_24_hr));
            } else {
                final String time = date.toString(context.getString(R.string.format_timeline_event_time_12_hr));
                final String period = date.toString(context.getString(R.string.format_timeline_12_hr_period));
                return Styles.assembleReadingAndUnitWithSpace(time, period, Styles.UNIT_STYLE_SUBSCRIPT);
            }
        }
        return context.getString(R.string.format_date_placeholder);
    }

    @NotLocalizable(NotLocalizable.BecauseOf.API_LIMITATION)
    public
    @NonNull
    CharSequence formatAsAlarmTime(@Nullable LocalTime time, boolean use24Time) {
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

    public
    @NonNull
    String formatAsTime(@Nullable DateTime time, boolean use24Time) {
        if (time != null) {
            if (use24Time) {
                return time.toString(context.getString(R.string.format_time_24_hr));
            } else {
                return time.toString(context.getString(R.string.format_time_12_hr));
            }
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public
    @NonNull
    String formatAsDayAndTime(@Nullable DateTime time, boolean use24Time) {
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
    @NonNull
    public String formatAsRelativeTime(@Nullable final DateTime time) {
        if (time != null) {
            final DateTime now = DateTime.now(time.getZone()).withMillisOfSecond(0);
            final DateTime roundTime = time.withMillisOfSecond(0);
            if (now.isBefore(roundTime)) {
                Logger.warn(getClass().getSimpleName(), "formatAsRelativeTime not meant to be used with dates in the past");
                return formatAsLocalizedDate(roundTime.toLocalDate());
            }

            final Interval interval = new Interval(roundTime, now);
            final int count;
            final @StringRes int typeRes;
            if (Days.daysIn(interval).isLessThan(Days.ONE)) {
                return context.getResources().getString(R.string.action_today);
            }
            if (Days.daysIn(interval).isLessThan(Days.TWO)) {
                return context.getResources().getString(R.string.action_yesterday);
            }
            if (Days.daysIn(interval).isLessThan(Days.SEVEN)) {
                count = Days.daysIn(interval).getDays();
                typeRes = R.string.format_day;
            } else if (Weeks.weeksIn(interval).isLessThan(Weeks.weeks(9))) {
                count = Weeks.weeksIn(interval).getWeeks();
                typeRes = R.string.format_week;
            } else if (Months.monthsIn(interval).isLessThan(Months.TWELVE)) {
                count = Months.monthsIn(interval).getMonths();
                typeRes = R.string.format_month;
            } else {
                count = Years.yearsIn(interval).getYears();
                typeRes = R.string.format_year;
            }
            return context.getResources().getString(R.string.format_ago, count, context.getString(typeRes));
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
    }

    public
    @NonNull
    CharSequence formatDuration(final long duration,
                                @NonNull final TimeUnit unit) {
        final long totalMinutes = unit.toMinutes(duration);
        if (totalMinutes < 60) {
            return Styles.assembleReadingAndUnitWithSpace(Long.toString(totalMinutes),
                                                          context.getString(R.string.format_duration_abbrev_minutes),
                                                          Styles.UNIT_STYLE_SUBSCRIPT);
        } else {
            final float hours = totalMinutes / 60f;
            final long leftOverMinutes = totalMinutes % 60;

            final String reading;
            if (leftOverMinutes == 0) {
                reading = String.format("%.0f", hours);
            } else {
                reading = String.format("%.1f", hours);
            }

            return Styles.assembleReadingAndUnitWithSpace(reading,
                                                          context.getString(R.string.format_duration_abbrev_hours),
                                                          Styles.UNIT_STYLE_SUBSCRIPT);
        }
    }

    //endregion


    //region Calendar Support

    public static
    @JodaWeekDay
    int calendarDayToJodaTimeDay(int calendarDay) {
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

    public static
    @JodaWeekDay
    int nextJodaTimeDay(@JodaWeekDay final int jodaTimeDay) {
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

    /**
     * Get the name of the day that the given months first day is on as an integer.
     *
     * @param month - int representing january, february, ... , december.
     * @return int that represents Sun-Sat (1-7)
     * @throws ParseException
     */
    public static int getFirstDayOfMonthValue(final int month) throws ParseException {
        final Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.MONTH) < month) {
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
        }
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.MONTH, month);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * Convert a 3 char month string into its calendar int value. JAN -> 0
     *
     * @param month 3 char month string for a given month. JAN, FEB, ... , DEC
     * @return int representing month.
     * @throws ParseException
     */
    public static int getMonthInt(@NonNull String month) throws ParseException {
        Calendar cal = Calendar.getInstance();
        //todo consider additional languages.
        cal.setTime(new SimpleDateFormat("MMM", Locale.ENGLISH).parse(month));
        return cal.get(Calendar.MONTH);
    }

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
    public @interface JodaWeekDay {
    }
}
