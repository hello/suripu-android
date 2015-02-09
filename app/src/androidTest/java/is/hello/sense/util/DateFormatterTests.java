package is.hello.sense.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import is.hello.sense.graph.InjectionTestCase;

public class DateFormatterTests extends InjectionTestCase {
    private DateFormatter formatter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (formatter == null) {
            this.formatter = new DateFormatter(getInstrumentation().getTargetContext());
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
        assertEquals("Last Night", formatter.formatAsTimelineDate(DateFormatter.lastNight()));

        DateTime nightBefore = DateFormatter.lastNight().minusDays(1);
        assertEquals(nightBefore.toString("EEEE"), formatter.formatAsTimelineDate(nightBefore));

        DateTime weekBefore = DateFormatter.lastNight().minusDays(8);
        assertEquals(weekBefore.toString("MMMM d"), formatter.formatAsTimelineDate(weekBefore));

        assertEquals("--", formatter.formatAsTimelineDate(null));
    }

    public void testFormatAsBirthDate() {
        assertEquals("02/03/01", formatter.formatAsBirthDate(new LocalDate(2001, 2, 3)));
        assertEquals("--", formatter.formatAsBirthDate(null));
    }

    public void testFormatAsTimelineStamp() {
        assertEquals("2:30", formatter.formatAsTimelineStamp(new DateTime(2001, 2, 3, 14, 30), false));
        assertEquals("14:30", formatter.formatAsTimelineStamp(new DateTime(2001, 2, 3, 14, 30), true));
        assertEquals("--", formatter.formatAsTimelineStamp(null, false));
        assertEquals("--", formatter.formatAsTimelineStamp(null, true));
    }

    public void testFormatAsTime() {
        assertEquals("2:30 PM", formatter.formatAsTime(new DateTime(2001, 2, 3, 14, 30), false));
        assertEquals("14:30", formatter.formatAsTime(new DateTime(2001, 2, 3, 14, 30), true));
        assertEquals("--", formatter.formatAsTime((DateTime) null, false));
        assertEquals("--", formatter.formatAsTime((DateTime) null, true));
    }

    public void testFormatAsDayAndTime() {
        assertEquals("Saturday – 2:30 PM", formatter.formatAsDayAndTime(new DateTime(2001, 2, 3, 14, 30), false));
        assertEquals("Saturday – 14:30", formatter.formatAsDayAndTime(new DateTime(2001, 2, 3, 14, 30), true));
        assertEquals("--", formatter.formatAsDayAndTime(null, false));
        assertEquals("--", formatter.formatAsDayAndTime(null, true));
    }
}
