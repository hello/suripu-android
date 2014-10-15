package is.hello.sense.util;

import android.test.InstrumentationTestCase;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

public class DateFormatterTests extends InstrumentationTestCase {
    private static final LocalDateTime LOCAL_TEST_TIME = new LocalDateTime(2014, 10, 1, 10, 30, 0);
    private static final DateTime TEST_TIME = new DateTime(2014, 10, 1, 10, 30, 0);
    private DateFormatter formatter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.formatter = new DateFormatter(getInstrumentation().getTargetContext());
    }

    public void testTimelineDate() {
        assertNotNull(formatter.formatAsTimelineDate(null));
    }

    public void testFormatAsBirthDate() {
        assertNotNull(formatter.formatAsBirthDate(null));
        assertEquals("10/01/14", formatter.formatAsBirthDate(TEST_TIME));
    }

    public void testFormatAsDate() {
        assertNotNull(formatter.formatAsDate((DateTime) null));
        assertNotNull(formatter.formatAsDate((LocalDateTime) null));
        assertEquals("October 1", formatter.formatAsDate(TEST_TIME));
        assertEquals("October 1", formatter.formatAsDate(LOCAL_TEST_TIME));
    }

    public void testFormatAsTime() {
        assertNotNull(formatter.formatAsTime((DateTime) null, false));
        assertNotNull(formatter.formatAsTime((LocalDateTime) null, false));
        assertEquals("10:30 AM", formatter.formatAsTime(TEST_TIME, false));
        assertEquals("10:30", formatter.formatAsTime(TEST_TIME, true));
    }
}
