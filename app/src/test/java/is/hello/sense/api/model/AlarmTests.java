package is.hello.sense.api.model;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.DateFormatter;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class AlarmTests extends SenseTestCase {

    private Alarm generateTestAlarm(final boolean isSmart,
                                    final boolean isEnabled) {
        final Alarm alarm = new Alarm();
        alarm.setSmart(isSmart);
        alarm.setEnabled(isEnabled);
        return alarm;
    }

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

    @Test
    public void newAlarmCanBeSmartWithExistingDisabledSmartAlarmOrEnabledNonSmartAlarms() {
        final Alarm testAlarm = new Alarm();

        final Alarm existingAlarm = generateTestAlarm(false, true);

        final Alarm existingAlarm2 = generateTestAlarm(true, false);

        final List<Alarm> existingAlarms = new ArrayList<>();
        existingAlarms.add(existingAlarm);
        existingAlarms.add(existingAlarm2);

        assertTrue(testAlarm.canBeSmartWith(existingAlarms));
    }

    @Test
    public void newAlarmCanBeSmartWithExistingEnabledSmartAlarmWithDifferentDayOfWeek() {
        final Alarm testAlarm = new Alarm();

        final Alarm existingSmartAlarm = generateTestAlarm(true, true);
        // different day from default
        existingSmartAlarm.setDaysOfWeek(
                Collections.singletonList(testAlarm.getDefaultRingTime()
                                                            .plusDays(1)
                                                            .getDayOfWeek()));

        final List<Alarm> existingAlarms = new ArrayList<>();
        existingAlarms.add(existingSmartAlarm);

        assertTrue(testAlarm.canBeSmartWith(existingAlarms));
    }

    @Test
    public void existingAlarmCanBeSmartWithExistingEnabledSmartAlarmWithDifferentDayOfWeek() {
        final Alarm testAlarm = new Alarm();
        testAlarm.addDayOfWeek(DateTimeConstants.MONDAY);
        testAlarm.addDayOfWeek(DateTimeConstants.WEDNESDAY);
        testAlarm.addDayOfWeek(DateTimeConstants.FRIDAY);

        final Alarm existingSmartAlarm = generateTestAlarm(true, true);
        // different day
        existingSmartAlarm.addDayOfWeek(DateTimeConstants.TUESDAY);

        final List<Alarm> existingAlarms = new ArrayList<>();
        existingAlarms.add(existingSmartAlarm);

        assertTrue(testAlarm.canBeSmartWith(existingAlarms));
    }

    //region Sad Path

    @Test
    public void newAlarmCannotBeSmartWithExistingNonRepeatingEnabledSmartAlarm() {
        final Alarm testAlarm = new Alarm();

        final Alarm existingSmartAlarm = generateTestAlarm(true, true);
        // no days of week
        existingSmartAlarm.setDaysOfWeek(new ArrayList<>(0));

        final List<Alarm> existingAlarms = new ArrayList<>();
        existingAlarms.add(existingSmartAlarm);

        assertFalse(testAlarm.canBeSmartWith(existingAlarms));
    }

    @Test
    public void newAlarmCannotBeSmartWithExistingEnabledSmartAlarmWithSameDayOfWeek() {
        final Alarm testAlarm = new Alarm();

        final Alarm existingSmartAlarm = generateTestAlarm(true, true);
        // all days of week
        existingSmartAlarm.setDaysOfWeek(DateFormatter.getDaysOfWeek(DateTimeConstants.MONDAY));

        final List<Alarm> existingAlarms = new ArrayList<>();
        existingAlarms.add(existingSmartAlarm);

        assertFalse(testAlarm.canBeSmartWith(existingAlarms));
    }

    @Test
    public void existingAlarmCannotBeSmartWithExistingEnabledSmartAlarmWithSameDayOfWeek() {
        final Alarm testAlarm = new Alarm();
        @DateFormatter.JodaWeekDay final int SAME_DAY = DateTimeConstants.WEDNESDAY;
        testAlarm.addDayOfWeek(DateTimeConstants.MONDAY);
        testAlarm.addDayOfWeek(SAME_DAY);

        final Alarm existingSmartAlarm = generateTestAlarm(true, true);

        existingSmartAlarm.addDayOfWeek(SAME_DAY);
        existingSmartAlarm.addDayOfWeek(DateTimeConstants.FRIDAY);

        final List<Alarm> existingAlarms = new ArrayList<>();
        existingAlarms.add(existingSmartAlarm);

        assertFalse(testAlarm.canBeSmartWith(existingAlarms));
    }

    @Test
    public void existingAlarmWithDefaultDayCannotBeSmartWithExistingNonRepeatingEnabledSmartAlarm() {
        final Alarm testAlarm = new Alarm();
        @DateFormatter.JodaWeekDay final int DEFAULT_DAY = testAlarm.getDefaultRingTime().getDayOfWeek();
        testAlarm.addDayOfWeek(DEFAULT_DAY);

        final Alarm existingSmartAlarm = generateTestAlarm(true, true);

        existingSmartAlarm.setDaysOfWeek(Collections.emptyList());

        final List<Alarm> existingAlarms = new ArrayList<>();
        existingAlarms.add(existingSmartAlarm);

        assertFalse(testAlarm.canBeSmartWith(existingAlarms));
    }

    //endregion
}
