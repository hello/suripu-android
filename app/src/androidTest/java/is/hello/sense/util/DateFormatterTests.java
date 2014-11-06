package is.hello.sense.util;

import android.test.InstrumentationTestCase;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import is.hello.sense.R;

public class DateFormatterTests extends InstrumentationTestCase {
    private static final LocalDateTime TEST_LOCAL_DATETIME = new LocalDateTime(2014, 10, 1, 10, 30, 0);
    private static final LocalDate TEST_LOCAL_DATE = new LocalDate(2014, 10, 1);
    private static final DateTime TEST_DATETIME = new DateTime(2014, 10, 1, 10, 30, 0);
    private static final LocalTime TEST_LOCAL_TIME = new LocalTime(10, 30, 0);
    private DateFormatter formatter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.formatter = new DateFormatter(getInstrumentation().getTargetContext());
    }

    public void testTimelineDate() {
        assertNotNull(formatter.formatAsTimelineDate(null));
        String lastNightText = getInstrumentation().getTargetContext().getString(R.string.format_date_last_night);
        assertEquals(lastNightText, formatter.formatAsTimelineDate(DateFormatter.lastNight()));
        assertFalse(lastNightText.equals(formatter.formatAsTimelineDate(DateFormatter.lastNight().minusDays(1))));
    }

    public void testFormatAsBirthDate() {
        assertNotNull(formatter.formatAsBirthDate(null));
        assertEquals("10/01/14", formatter.formatAsBirthDate(TEST_LOCAL_DATE));
    }

    public void testFormatAsDate() {
        assertNotNull(formatter.formatAsDate(null));
        assertEquals("October 1", formatter.formatAsDate(TEST_DATETIME));
    }

    public void testFormatAsTime() {
        assertNotNull(formatter.formatAsTime((LocalTime) null, false));
        assertNotNull(formatter.formatAsTime((LocalDateTime) null, false));
        assertNotNull(formatter.formatAsTime((DateTime) null, false));
        assertEquals("10:30 AM", formatter.formatAsTime(TEST_LOCAL_DATETIME, false));
        assertEquals("10:30", formatter.formatAsTime(TEST_LOCAL_DATETIME, true));
        assertEquals("10:30 AM", formatter.formatAsTime(TEST_LOCAL_TIME, false));
        assertEquals("10:30", formatter.formatAsTime(TEST_LOCAL_TIME, true));
        assertEquals("10:30 AM", formatter.formatAsTime(TEST_DATETIME, false));
        assertEquals("10:30", formatter.formatAsTime(TEST_DATETIME, true));
    }
}
