package is.hello.sense.api.model;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.Collections;

import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AlarmTests extends SenseTestCase {
    @Test
    public void repeated() {
        final Alarm alarm = new Alarm();
        alarm.setTime(new LocalTime(8, 0));
        alarm.setRingOnce();

        assertThat(alarm.isRepeated(), is(false));

        alarm.addDayOfWeek(DateTimeConstants.SATURDAY);
        alarm.addDayOfWeek(DateTimeConstants.SUNDAY);

        assertThat(alarm.isRepeated(), is(true));

        alarm.removeDayOfWeek(DateTimeConstants.SATURDAY);
        alarm.removeDayOfWeek(DateTimeConstants.SUNDAY);

        assertThat(alarm.isRepeated(), is(false));

        alarm.setDaysOfWeek(Lists.newArrayList(DateTimeConstants.MONDAY,
                                               DateTimeConstants.TUESDAY));

        assertThat(alarm.isRepeated(), is(true));

        alarm.setDaysOfWeek(Collections.emptyList());

        assertThat(alarm.isRepeated(), is(false));
    }
}
