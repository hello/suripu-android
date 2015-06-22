package is.hello.sense.util;

import android.content.Context;
import android.text.format.DateFormat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import is.hello.sense.R;
import is.hello.sense.graph.InjectionTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DateFormatterTests extends InjectionTests {
    private DateFormatter formatter;
    private String placeholder;

    @Before
    public void initialize() throws Exception {
        if (formatter == null) {
            Context targetContext = getContext();
            this.formatter = new DateFormatter(targetContext);
            this.placeholder = targetContext.getString(R.string.format_date_placeholder);
        }
    }

    @Test
    public void now() {
        assertEquals(DateTimeZone.getDefault(), DateFormatter.now().getZone());
    }

    @Test
    public void lastNight() {
        assertEquals(DateTimeZone.getDefault(), DateFormatter.lastNight().getZone());
    }

    @Test
    public void isLastNight() {
        assertTrue(DateFormatter.isLastNight(DateFormatter.lastNight()));
        assertFalse(DateFormatter.isLastNight(DateFormatter.now()));
    }

    @Test
    public void isInLastWeek() {
        DateTime now = DateFormatter.now();
        assertFalse(DateFormatter.isInLastWeek(now.plusDays(1)));
        assertFalse(DateFormatter.isInLastWeek(now));
        assertTrue(DateFormatter.isInLastWeek(now.minusDays(1)));
        assertTrue(DateFormatter.isInLastWeek(now.minusDays(2)));
        assertTrue(DateFormatter.isInLastWeek(now.minusDays(3)));
        assertTrue(DateFormatter.isInLastWeek(now.minusDays(4)));
        assertTrue(DateFormatter.isInLastWeek(now.minusDays(5)));
        assertTrue(DateFormatter.isInLastWeek(now.minusDays(6)));
        assertTrue(DateFormatter.isInLastWeek(now.minusDays(7)));
        assertFalse(DateFormatter.isInLastWeek(now.minusDays(8)));
    }

    @Test
    public void formatAsTimelineDate() {
        String lastNightString = getContext().getString(R.string.format_date_last_night);
        assertEquals(lastNightString, formatter.formatAsTimelineDate(DateFormatter.lastNight()));

        DateTime nightBefore = DateFormatter.lastNight().minusDays(1);
        assertEquals(nightBefore.toString("EEEE"), formatter.formatAsTimelineDate(nightBefore));

        DateTime weekBefore = DateFormatter.lastNight().minusDays(8);
        assertEquals(weekBefore.toString("MMMM d"), formatter.formatAsTimelineDate(weekBefore));

        assertEquals(placeholder, formatter.formatAsTimelineDate(null));
    }

    @Test
    public void formatAsBirthDate() {
        GregorianCalendar calendar = new GregorianCalendar(2001, 8, 3); // Months are 0-indexed in the Java API
        Date canonicalDate = new Date(calendar.getTimeInMillis());
        Context context = getContext();
        String canonicalString = DateFormat.getDateFormat(context).format(canonicalDate);
        assertEquals(canonicalString, formatter.formatAsLocalizedDate(new LocalDate(2001, 9, 3)));
        assertEquals(placeholder, formatter.formatAsLocalizedDate(null));
    }

    @Test
    public void formatForTimelineEvent() {
        assertEquals("2:30 PM", formatter.formatForTimelineEvent(new DateTime(2001, 2, 3, 14, 30), false).toString());
        assertEquals("14:30", formatter.formatForTimelineEvent(new DateTime(2001, 2, 3, 14, 30), true).toString());
        assertEquals(placeholder, formatter.formatForTimelineEvent(null, false));
        assertEquals(placeholder, formatter.formatForTimelineEvent(null, true));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void formatForTimelineSegment() {
        assertEquals("2 PM", formatter.formatForTimelineSegment(new LocalTime(14, 30), false).toString());
        assertEquals("14:00", formatter.formatForTimelineSegment(new LocalTime(14, 30), true).toString());
        assertNull(formatter.formatForTimelineSegment(null, false));
        assertNull(formatter.formatForTimelineSegment(null, true));
    }

    @Test
    public void formatForTimelineInfo() {
        assertEquals("2:30 PM", formatter.formatForTimelineInfo(new DateTime(2001, 2, 3, 14, 30), false).toString());
        assertEquals("14:30", formatter.formatForTimelineInfo(new DateTime(2001, 2, 3, 14, 30), true).toString());
        assertEquals(placeholder, formatter.formatForTimelineInfo(null, false));
        assertEquals(placeholder, formatter.formatForTimelineInfo(null, true));
    }

    @Test
    public void formatAsTime() {
        assertEquals("2:30 PM", formatter.formatAsTime(new DateTime(2001, 2, 3, 14, 30), false));
        assertEquals("14:30", formatter.formatAsTime(new DateTime(2001, 2, 3, 14, 30), true));
        assertEquals(placeholder, formatter.formatAsTime((DateTime) null, false));
        assertEquals(placeholder, formatter.formatAsTime((DateTime) null, true));
    }

    @Test
    public void formatAsDayAndTime() {
        assertEquals("Saturday – 2:30 PM", formatter.formatAsDayAndTime(new DateTime(2001, 2, 3, 14, 30), false));
        assertEquals("Saturday – 14:30", formatter.formatAsDayAndTime(new DateTime(2001, 2, 3, 14, 30), true));
        assertEquals(placeholder, formatter.formatAsDayAndTime(null, false));
        assertEquals(placeholder, formatter.formatAsDayAndTime(null, true));
    }

    @Test
    public void formatAsRelativeTime() {
        assertEquals("10 seconds ago", formatter.formatAsRelativeTime(DateTime.now().minusSeconds(10)));
        assertEquals("10 minutes ago", formatter.formatAsRelativeTime(DateTime.now().minusMinutes(10)));
        assertEquals("10 hours ago", formatter.formatAsRelativeTime(DateTime.now().minusHours(10)));
        assertEquals("5 days ago", formatter.formatAsRelativeTime(DateTime.now().minusDays(5)));
        assertEquals("1 week ago", formatter.formatAsRelativeTime(DateTime.now().minusDays(10)));
        assertEquals("10 months ago", formatter.formatAsRelativeTime(DateTime.now().minusMonths(10)));

        DateTime twoYearsAgo = DateTime.now().minusYears(2);
        assertEquals(formatter.formatAsLocalizedDate(twoYearsAgo.toLocalDate()), formatter.formatAsRelativeTime(twoYearsAgo));
    }

    @Test
    public void formatDuration() {
        assertEquals("0 min", formatter.formatDuration(30, TimeUnit.SECONDS).toString());
        assertEquals("1 min", formatter.formatDuration(60, TimeUnit.SECONDS).toString());
        assertEquals("5 min", formatter.formatDuration(5, TimeUnit.MINUTES).toString());
        assertEquals("1 hr", formatter.formatDuration(60, TimeUnit.MINUTES).toString());
        assertEquals("1.5 hr", formatter.formatDuration(90, TimeUnit.MINUTES).toString());
        assertEquals("1.7 hr", formatter.formatDuration(100, TimeUnit.MINUTES).toString());
        assertEquals("2 hr", formatter.formatDuration(120, TimeUnit.MINUTES).toString());
    }
}
