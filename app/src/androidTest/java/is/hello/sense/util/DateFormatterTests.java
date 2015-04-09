package is.hello.sense.util;

import android.content.Context;
import android.text.format.DateFormat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import java.util.Date;
import java.util.GregorianCalendar;

import is.hello.sense.R;
import is.hello.sense.graph.InjectionTestCase;

public class DateFormatterTests extends InjectionTestCase {
    private DateFormatter formatter;
    private String placeholder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (formatter == null) {
            this.formatter = new DateFormatter(getInstrumentation().getTargetContext());
            this.placeholder = getInstrumentation().getTargetContext().getString(R.string.format_date_placeholder);
        }
    }

    public void testNow() {
        assertEquals(DateTimeZone.getDefault(), DateFormatter.now().getZone());
    }

    public void testLastNight() {
        assertEquals(DateTimeZone.getDefault(), DateFormatter.lastNight().getZone());
    }

    public void testIsLastNight() {
        assertTrue(DateFormatter.isLastNight(DateFormatter.lastNight()));
        assertFalse(DateFormatter.isLastNight(DateFormatter.now()));
    }

    public void testIsInLastWeek() {
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

    public void testFormatAsTimelineDate() {
        String lastNightString = getInstrumentation().getTargetContext().getString(R.string.format_date_last_night);
        assertEquals(lastNightString, formatter.formatAsTimelineDate(DateFormatter.lastNight()));

        DateTime nightBefore = DateFormatter.lastNight().minusDays(1);
        assertEquals(nightBefore.toString("EEEE"), formatter.formatAsTimelineDate(nightBefore));

        DateTime weekBefore = DateFormatter.lastNight().minusDays(8);
        assertEquals(weekBefore.toString("MMMM d"), formatter.formatAsTimelineDate(weekBefore));

        assertEquals(placeholder, formatter.formatAsTimelineDate(null));
    }

    public void testFormatAsBirthDate() {
        GregorianCalendar calendar = new GregorianCalendar(2001, 8, 3); // Months are 0-indexed in the Java API
        Date canonicalDate = new Date(calendar.getTimeInMillis());
        Context context = getInstrumentation().getTargetContext();
        String canonicalString = DateFormat.getDateFormat(context).format(canonicalDate);
        assertEquals(canonicalString, formatter.formatAsLocalizedDate(new LocalDate(2001, 9, 3)));
        assertEquals(placeholder, formatter.formatAsLocalizedDate(null));
    }

    public void testFormatAsTimelineStamp() {
        assertEquals("2:30", formatter.formatAsTimelineStamp(new DateTime(2001, 2, 3, 14, 30), false));
        assertEquals("14:30", formatter.formatAsTimelineStamp(new DateTime(2001, 2, 3, 14, 30), true));
        assertEquals(placeholder, formatter.formatAsTimelineStamp(null, false));
        assertEquals(placeholder, formatter.formatAsTimelineStamp(null, true));
    }

    public void testFormatAsTime() {
        assertEquals("2:30 PM", formatter.formatAsTime(new DateTime(2001, 2, 3, 14, 30), false));
        assertEquals("14:30", formatter.formatAsTime(new DateTime(2001, 2, 3, 14, 30), true));
        assertEquals(placeholder, formatter.formatAsTime((DateTime) null, false));
        assertEquals(placeholder, formatter.formatAsTime((DateTime) null, true));
    }

    public void testFormatAsDayAndTime() {
        assertEquals("Saturday – 2:30 PM", formatter.formatAsDayAndTime(new DateTime(2001, 2, 3, 14, 30), false));
        assertEquals("Saturday – 14:30", formatter.formatAsDayAndTime(new DateTime(2001, 2, 3, 14, 30), true));
        assertEquals(placeholder, formatter.formatAsDayAndTime(null, false));
        assertEquals(placeholder, formatter.formatAsDayAndTime(null, true));
    }

    public void testFormatAsRelativeTime() {
        assertEquals("10 seconds ago", formatter.formatAsRelativeTime(DateTime.now().minusSeconds(10)));
        assertEquals("10 minutes ago", formatter.formatAsRelativeTime(DateTime.now().minusMinutes(10)));
        assertEquals("10 hours ago", formatter.formatAsRelativeTime(DateTime.now().minusHours(10)));
        assertEquals("5 days ago", formatter.formatAsRelativeTime(DateTime.now().minusDays(5)));
        assertEquals("1 week ago", formatter.formatAsRelativeTime(DateTime.now().minusDays(10)));
        assertEquals("10 months ago", formatter.formatAsRelativeTime(DateTime.now().minusMonths(10)));

        DateTime twoYearsAgo = DateTime.now().minusYears(2);
        assertEquals(formatter.formatAsLocalizedDate(twoYearsAgo.toLocalDate()), formatter.formatAsRelativeTime(twoYearsAgo));
    }
}
