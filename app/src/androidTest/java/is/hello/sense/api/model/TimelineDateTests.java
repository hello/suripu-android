package is.hello.sense.api.model;

import junit.framework.TestCase;

import static is.hello.sense.TestUtils.assertThrows;

public class TimelineDateTests extends TestCase {
    public void testToday() {
        TimelineDate today = TimelineDate.today();
        assertNotNull(today);
    }

    public void testParsing() {
        TimelineDate testDate = TimelineDate.fromString("10-01-14");
        assertNotNull(testDate);
        assertEquals("10", testDate.month);
        assertEquals("01", testDate.day);
        assertEquals("14", testDate.year);
    }

    public void testConstraints() {
        assertThrows(() -> new TimelineDate("10", "10", "2014"));
        assertThrows(() -> new TimelineDate("10", "10", "204"));
    }

    public void testToString() {
        TimelineDate testDate = new TimelineDate("10", "08", "14");
        assertEquals("10-08-14", testDate.toString());
    }
}
