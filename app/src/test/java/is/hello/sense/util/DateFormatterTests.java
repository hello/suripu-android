package is.hello.sense.util;

import android.text.format.DateFormat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import is.hello.sense.R;
import is.hello.sense.graph.InjectionTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class DateFormatterTests extends InjectionTestCase {
    private final DateFormatter formatter;
    private final String placeholder;


    //region Lifecycle

    public DateFormatterTests() {
        this.formatter = new DateFormatter(getContext());
        this.placeholder = getString(R.string.format_date_placeholder);
    }

    @After
    public void tearDown() throws Exception {
        DateTimeUtils.setCurrentMillisSystem();
    }

    //endregion


    //region Last Night

    @Test
    public void nowDateTime() {
        assertThat(DateFormatter.nowDateTime().getZone(), is(equalTo(DateTimeZone.getDefault())));
    }

    @Test
    public void todayForTimeline() {
        final DateTime beforeBoundary = new DateTime(1969, 7, 21, 2, 0);
        DateTimeUtils.setCurrentMillisFixed(beforeBoundary.getMillis());

        final LocalDate lastNightBeforeBoundary = DateFormatter.todayForTimeline();
        assertThat(lastNightBeforeBoundary.getDayOfMonth(), is(equalTo(20)));


        final DateTime afterBoundary = new DateTime(1969, 7, 21, 5, 0);
        DateTimeUtils.setCurrentMillisFixed(afterBoundary.getMillis());

        final LocalDate lastNightAfterBoundary = DateFormatter.todayForTimeline();
        assertThat(lastNightAfterBoundary.getDayOfMonth(), is(equalTo(21)));
    }

    @Test
    public void lastNight() {
        final DateTime fixedPoint = new DateTime(1969, 7, 21, 5, 0);
        DateTimeUtils.setCurrentMillisFixed(fixedPoint.getMillis());

        final LocalDate lastNight = DateFormatter.lastNight();
        assertThat(lastNight.getDayOfMonth(), is(equalTo(20)));
    }

    @Test
    public void lastNightBoundary() {
        final DateTime beforeBoundary = new DateTime(1969, 7, 21, 2, 0);
        DateTimeUtils.setCurrentMillisFixed(beforeBoundary.getMillis());

        final LocalDate lastNightBeforeBoundary = DateFormatter.lastNight();
        assertThat(lastNightBeforeBoundary.getDayOfMonth(), is(equalTo(19)));


        final DateTime afterBoundary = new DateTime(1969, 7, 21, 5, 0);
        DateTimeUtils.setCurrentMillisFixed(afterBoundary.getMillis());

        final LocalDate lastNightAfterBoundary = DateFormatter.lastNight();
        assertThat(lastNightAfterBoundary.getDayOfMonth(), is(equalTo(20)));
    }

    @Test
    public void isLastNight() {
        assertThat(DateFormatter.isLastNight(DateFormatter.lastNight()), is(true));
        assertThat(DateFormatter.isLastNight(DateFormatter.lastNight().minusDays(5)), is(false));


        final DateTime beforeBoundary = new DateTime(1969, 7, 21, 2, 0);
        DateTimeUtils.setCurrentMillisFixed(beforeBoundary.getMillis());

        assertThat(DateFormatter.isLastNight(new LocalDate(1969, 7, 19)), is(true));
        assertThat(DateFormatter.isLastNight(new LocalDate(1969, 7, 20)), is(false));


        final DateTime afterBoundary = new DateTime(1969, 7, 21, 5, 0);
        DateTimeUtils.setCurrentMillisFixed(afterBoundary.getMillis());

        assertThat(DateFormatter.isLastNight(new LocalDate(1969, 7, 19)), is(false));
        assertThat(DateFormatter.isLastNight(new LocalDate(1969, 7, 20)), is(true));
    }

    //endregion


    //region Formatting

    @Test
    public void isInLastWeek() {
        LocalDate now = DateFormatter.nowLocalDate();
        assertThat(DateFormatter.isInLastWeek(now.plusDays(1)), is(false));
        assertThat(DateFormatter.isInLastWeek(now), is(false));
        assertThat(DateFormatter.isInLastWeek(now.minusDays(1)), is(true));
        assertThat(DateFormatter.isInLastWeek(now.minusDays(2)), is(true));
        assertThat(DateFormatter.isInLastWeek(now.minusDays(3)), is(true));
        assertThat(DateFormatter.isInLastWeek(now.minusDays(4)), is(true));
        assertThat(DateFormatter.isInLastWeek(now.minusDays(5)), is(true));
        assertThat(DateFormatter.isInLastWeek(now.minusDays(6)), is(true));
        assertThat(DateFormatter.isInLastWeek(now.minusDays(7)), is(true));
        assertThat(DateFormatter.isInLastWeek(now.minusDays(8)), is(false));
    }

    @Test
    public void formatAsTimelineDate() {
        String lastNightString = getString(R.string.format_date_last_night);
        assertThat(formatter.formatAsTimelineDate(DateFormatter.lastNight()),
                   is(equalTo(lastNightString)));

        LocalDate nightBefore = DateFormatter.lastNight().minusDays(1);
        assertThat(formatter.formatAsTimelineDate(nightBefore),
                   is(equalTo(nightBefore.toString("EEEE"))));

        LocalDate weekBefore = DateFormatter.lastNight().minusDays(8);
        assertThat(formatter.formatAsTimelineDate(weekBefore),
                   is(equalTo(weekBefore.toString("MMMM d"))));

        assertThat(formatter.formatAsTimelineDate(null),
                   is(equalTo(placeholder)));
    }

    @Test
    public void formatAsBirthDate() {
        final GregorianCalendar calendar = new GregorianCalendar(2001, 8, 3); // Months are 0-indexed in the Java API
        final Date canonicalDate = new Date(calendar.getTimeInMillis());
        final String canonicalString = DateFormat.getDateFormat(getContext())
                                                 .format(canonicalDate);

        assertThat(formatter.formatAsLocalizedDate(new LocalDate(2001, 9, 3)),
                   is(equalTo(canonicalString)));
        assertThat(formatter.formatAsLocalizedDate(null),
                   is(equalTo(placeholder)));
    }

    @Test
    public void formatForTimelineEvent() {
        assertThat(formatter.formatForTimelineEvent(new DateTime(2001, 2, 3, 14, 30), false).toString(),
                   is(equalTo("2:30 PM")));
        assertThat(formatter.formatForTimelineEvent(new DateTime(2001, 2, 3, 14, 30), true).toString(),
                   is(equalTo("14:30")));
        assertThat(formatter.formatForTimelineEvent(null, false),
                   is(equalTo(placeholder)));
        assertThat(formatter.formatForTimelineEvent(null, true),
                   is(equalTo(placeholder)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void formatForTimelineSegment() {
        assertThat(formatter.formatForTimelineSegment(new LocalTime(14, 30), false).toString(),
                   is(equalTo("2 PM")));
        assertThat(formatter.formatForTimelineSegment(new LocalTime(14, 30), true).toString(),
                   is(equalTo("14:00")));
        assertThat(formatter.formatForTimelineSegment(null, false),
                   is(nullValue()));
        assertThat(formatter.formatForTimelineSegment(null, true),
                   is(nullValue()));
    }

    @Test
    public void formatForTimelineInfo() {
        assertThat(formatter.formatForTimelineInfo(new DateTime(2001, 2, 3, 14, 30), false).toString(),
                   is(equalTo("2:30 PM")));
        assertThat(formatter.formatForTimelineInfo(new DateTime(2001, 2, 3, 14, 30), true).toString(),
                   is(equalTo("14:30")));
        assertThat(formatter.formatForTimelineInfo(null, false),
                   is(equalTo(placeholder)));
        assertThat(formatter.formatForTimelineInfo(null, true),
                   is(equalTo(placeholder)));
    }

    @Test
    public void formatAsTime() {
        assertThat(formatter.formatAsTime(new DateTime(2001, 2, 3, 14, 30), false),
                   is(equalTo("2:30 PM")));
        assertThat(formatter.formatAsTime(new DateTime(2001, 2, 3, 14, 30), true),
                   is(equalTo("14:30")));
        assertThat(formatter.formatAsTime(null, false),
                   is(equalTo(placeholder)));
        assertThat(formatter.formatAsTime(null, true),
                   is(equalTo(placeholder)));
    }

    @Test
    public void formatAsDayAndTime() {
        assertThat(formatter.formatAsDayAndTime(new DateTime(2001, 2, 3, 14, 30), false),
                   is(equalTo("Saturday – 2:30 PM")));
        assertThat(formatter.formatAsDayAndTime(new DateTime(2001, 2, 3, 14, 30), true),
                   is(equalTo("Saturday – 14:30")));
        assertThat(formatter.formatAsDayAndTime(null, false),
                   is(equalTo(placeholder)));
        assertThat(formatter.formatAsDayAndTime(null, true),
                   is(equalTo(placeholder)));
    }

    @Test
    public void formatAsRelativeTime() {
        assertThat(formatter.formatAsRelativeTime(DateTime.now().minusSeconds(10)),
                   is(equalTo("10 seconds ago")));
        assertThat(formatter.formatAsRelativeTime(DateTime.now().minusMinutes(10)),
                   is(equalTo("10 minutes ago")));
        assertThat(formatter.formatAsRelativeTime(DateTime.now().minusHours(10)),
                   is(equalTo("10 hours ago")));
        assertThat(formatter.formatAsRelativeTime(DateTime.now().minusDays(5)),
                   is(equalTo("5 days ago")));
        assertThat(formatter.formatAsRelativeTime(DateTime.now().minusDays(10)),
                   is(equalTo("1 week ago")));
        assertThat(formatter.formatAsRelativeTime(DateTime.now().minusMonths(10)),
                   is(equalTo("10 months ago")));

        DateTime twoYearsAgo = DateTime.now().minusYears(2);
        assertThat(formatter.formatAsRelativeTime(twoYearsAgo),
                   is(equalTo(formatter.formatAsLocalizedDate(twoYearsAgo.toLocalDate()))));
    }

    @Test
    public void formatDuration() {
        assertThat(formatter.formatDuration(30, TimeUnit.SECONDS).toString(),
                   is(equalTo("0 min")));
        assertThat(formatter.formatDuration(60, TimeUnit.SECONDS).toString(),
                   is(equalTo("1 min")));
        assertThat(formatter.formatDuration(5, TimeUnit.MINUTES).toString(),
                   is(equalTo("5 min")));
        assertThat(formatter.formatDuration(60, TimeUnit.MINUTES).toString(),
                   is(equalTo("1 hr")));
        assertThat(formatter.formatDuration(90, TimeUnit.MINUTES).toString(),
                   is(equalTo("1.5 hr")));
        assertThat(formatter.formatDuration(100, TimeUnit.MINUTES).toString(),
                   is(equalTo("1.7 hr")));
        assertThat(formatter.formatDuration(120, TimeUnit.MINUTES).toString(),
                   is(equalTo("2 hr")));
    }

    //endregion


    //region Calendar Support

    @Test
    public void calendarDayToJodaTimeDay() {
        assertThat(DateFormatter.calendarDayToJodaTimeDay(Calendar.MONDAY),
                   is(equalTo(DateTimeConstants.MONDAY)));
        assertThat(DateFormatter.calendarDayToJodaTimeDay(Calendar.TUESDAY),
                   is(equalTo(DateTimeConstants.TUESDAY)));
        assertThat(DateFormatter.calendarDayToJodaTimeDay(Calendar.WEDNESDAY),
                   is(equalTo(DateTimeConstants.WEDNESDAY)));
        assertThat(DateFormatter.calendarDayToJodaTimeDay(Calendar.THURSDAY),
                   is(equalTo(DateTimeConstants.THURSDAY)));
        assertThat(DateFormatter.calendarDayToJodaTimeDay(Calendar.FRIDAY),
                   is(equalTo(DateTimeConstants.FRIDAY)));
        assertThat(DateFormatter.calendarDayToJodaTimeDay(Calendar.SATURDAY),
                   is(equalTo(DateTimeConstants.SATURDAY)));
        assertThat(DateFormatter.calendarDayToJodaTimeDay(Calendar.SUNDAY),
                   is(equalTo(DateTimeConstants.SUNDAY)));
    }

    @Test
    public void jodaTimeDayToCalendarDay() {
        assertThat(DateFormatter.jodaTimeDayToCalendarDay(DateTimeConstants.MONDAY),
                   is(equalTo(Calendar.MONDAY)));
        assertThat(DateFormatter.jodaTimeDayToCalendarDay(DateTimeConstants.TUESDAY),
                   is(equalTo(Calendar.TUESDAY)));
        assertThat(DateFormatter.jodaTimeDayToCalendarDay(DateTimeConstants.WEDNESDAY),
                   is(equalTo(Calendar.WEDNESDAY)));
        assertThat(DateFormatter.jodaTimeDayToCalendarDay(DateTimeConstants.THURSDAY),
                   is(equalTo(Calendar.THURSDAY)));
        assertThat(DateFormatter.jodaTimeDayToCalendarDay(DateTimeConstants.FRIDAY),
                   is(equalTo(Calendar.FRIDAY)));
        assertThat(DateFormatter.jodaTimeDayToCalendarDay(DateTimeConstants.SATURDAY),
                   is(equalTo(Calendar.SATURDAY)));
        assertThat(DateFormatter.jodaTimeDayToCalendarDay(DateTimeConstants.SUNDAY),
                   is(equalTo(Calendar.SUNDAY)));
    }

    @Test
    public void nextJodaTimeDay() {
        assertThat(DateFormatter.nextJodaTimeDay(DateTimeConstants.MONDAY),
                   is(equalTo(DateTimeConstants.TUESDAY)));
        assertThat(DateFormatter.nextJodaTimeDay(DateTimeConstants.TUESDAY),
                   is(equalTo(DateTimeConstants.WEDNESDAY)));
        assertThat(DateFormatter.nextJodaTimeDay(DateTimeConstants.WEDNESDAY),
                   is(equalTo(DateTimeConstants.THURSDAY)));
        assertThat(DateFormatter.nextJodaTimeDay(DateTimeConstants.THURSDAY),
                   is(equalTo(DateTimeConstants.FRIDAY)));
        assertThat(DateFormatter.nextJodaTimeDay(DateTimeConstants.FRIDAY),
                   is(equalTo(DateTimeConstants.SATURDAY)));
        assertThat(DateFormatter.nextJodaTimeDay(DateTimeConstants.SATURDAY),
                   is(equalTo(DateTimeConstants.SUNDAY)));
        assertThat(DateFormatter.nextJodaTimeDay(DateTimeConstants.SUNDAY),
                   is(equalTo(DateTimeConstants.MONDAY)));
    }
    @Test
    public void getDaysOfWeek() {
        assertThat(DateFormatter.getDaysOfWeek(DateTimeConstants.MONDAY),
                   hasItems(DateTimeConstants.MONDAY, DateTimeConstants.TUESDAY,
                            DateTimeConstants.WEDNESDAY, DateTimeConstants.THURSDAY,
                            DateTimeConstants.FRIDAY, DateTimeConstants.SATURDAY,
                            DateTimeConstants.SUNDAY));

        assertThat(DateFormatter.getDaysOfWeek(DateTimeConstants.SUNDAY),
                   hasItems(DateTimeConstants.SUNDAY, DateTimeConstants.MONDAY,
                            DateTimeConstants.TUESDAY, DateTimeConstants.WEDNESDAY,
                            DateTimeConstants.THURSDAY, DateTimeConstants.FRIDAY,
                            DateTimeConstants.SATURDAY));
    }

    //endregion
}
