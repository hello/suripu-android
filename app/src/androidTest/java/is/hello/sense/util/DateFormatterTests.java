package is.hello.sense.util;

import android.test.InstrumentationTestCase;

import org.joda.time.DateTime;

public class DateFormatterTests extends InstrumentationTestCase {
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

    public void testFormatAsDate() {
        assertNotNull(formatter.formatAsDate(null));
        assertEquals("October 1", formatter.formatAsDate(TEST_TIME));
    }

    public void testFormatAsTime() {
        assertNotNull(formatter.formatAsTime(null));
        assertEquals("10:30 AM", formatter.formatAsTime(TEST_TIME));
    }
}
