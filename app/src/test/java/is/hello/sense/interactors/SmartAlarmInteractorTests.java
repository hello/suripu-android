package is.hello.sense.interactors;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeUtils;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.model.Alarm;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Sync;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ConstantConditions")
public class SmartAlarmInteractorTests extends InjectionTestCase {
    @Inject
    SmartAlarmInteractor presenter;

    private Alarm generateTestAlarm(final boolean isSmart,
                                    final boolean isEnabled) {
        final Alarm alarm = new Alarm();
        alarm.setSmart(isSmart);
        alarm.setEnabled(isEnabled);
        return alarm;
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void update() throws Exception {
        final ArrayList<Alarm> smartAlarms = Sync.wrapAfter(presenter::update, presenter.alarms)
                                                 .last();
        assertThat(smartAlarms.size(), is(equalTo(1)));
    }

    @Test
    public void newAlarmCanBeSmartWithExistingDisabledSmartAlarmOrEnabledNonSmartAlarms() {
        final Alarm testAlarm = new Alarm();

        final Alarm existingAlarm = generateTestAlarm(false, true);

        final Alarm existingAlarm2 = generateTestAlarm(true, false);

        final List<Alarm> existingAlarms = new ArrayList<>();
        existingAlarms.add(existingAlarm);
        existingAlarms.add(existingAlarm2);

        assertThat(presenter.canBeSmartWith(testAlarm, existingAlarms), is(true));
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

        assertThat(presenter.canBeSmartWith(testAlarm, existingAlarms), is(true));
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

        assertThat(presenter.canBeSmartWith(testAlarm, existingAlarms), is(true));
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

        assertThat(presenter.canBeSmartWith(testAlarm, existingAlarms), is(false));
    }

    @Test
    public void newAlarmCannotBeSmartWithExistingEnabledSmartAlarmWithSameDayOfWeek() {
        final Alarm testAlarm = new Alarm();

        final Alarm existingSmartAlarm = generateTestAlarm(true, true);
        // all days of week
        existingSmartAlarm.setDaysOfWeek(DateFormatter.getDaysOfWeek(DateTimeConstants.MONDAY));

        final List<Alarm> existingAlarms = new ArrayList<>();
        existingAlarms.add(existingSmartAlarm);

        assertThat(presenter.canBeSmartWith(testAlarm, existingAlarms), is(false));
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

        assertThat(presenter.canBeSmartWith(testAlarm, existingAlarms), is(false));
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

        assertThat(presenter.canBeSmartWith(testAlarm, existingAlarms), is(false));
    }

    //endregion

    @Test
    public void validateAlarms() throws Exception {
        final Alarm sunday = new Alarm();
        sunday.addDayOfWeek(DateTimeConstants.SUNDAY);
        sunday.setSmart(true);

        final Alarm monday = new Alarm();
        monday.addDayOfWeek(DateTimeConstants.MONDAY);
        monday.setSmart(true);

        assertThat(presenter.validateAlarms(Lists.newArrayList(sunday, monday)), is(true));
        assertThat(presenter.validateAlarms(Lists.newArrayList(sunday, sunday)), is(false));
        assertThat(presenter.validateAlarms(Lists.newArrayList(monday, monday)), is(false));

        final Alarm stupidSunday = new Alarm();
        stupidSunday.setSmart(false);
        stupidSunday.addDayOfWeek(DateTimeConstants.SUNDAY);
        assertThat(presenter.validateAlarms(Lists.newArrayList(sunday, stupidSunday)), is(true));

        final Alarm disabledSunday = new Alarm();
        disabledSunday.addDayOfWeek(DateTimeConstants.SUNDAY);
        disabledSunday.setSmart(true);
        disabledSunday.setEnabled(false);
        assertThat(presenter.validateAlarms(Lists.newArrayList(sunday, disabledSunday)), is(true));
    }

    @Test
    public void save() throws Exception {
        final ArrayList<Alarm> goodAlarms = new ArrayList<>();
        Sync.wrap(presenter.save(goodAlarms))
            .assertThat(is(notNullValue()));

        final Alarm sunday = new Alarm();
        sunday.addDayOfWeek(DateTimeConstants.SUNDAY);
        sunday.setSmart(true);
        final ArrayList<Alarm> badAlarms = Lists.newArrayList(sunday, sunday);

        Sync.wrap(presenter.save(badAlarms))
            .assertThrows(SmartAlarmInteractor.DayOverlapError.class);
    }

    @Test
    public void isTooSoon() throws Exception {
        final Alarm alarm = new Alarm();

        final LocalTime midHour = new LocalTime(9, 30, 0);

        DateTimeUtils.setCurrentMillisFixed(new LocalTime(9, 30, 30).toDateTimeToday().getMillis());
        alarm.setTime(midHour);
        assertThat(presenter.isAlarmTooSoon(alarm), is(true));

        DateTimeUtils.setCurrentMillisFixed(midHour.toDateTimeToday().getMillis());
        alarm.setTime(midHour);
        assertThat(presenter.isAlarmTooSoon(alarm), is(true));

        alarm.setTime(midHour.plusMinutes(Alarm.TOO_SOON_MINUTES / 2));
        assertThat(presenter.isAlarmTooSoon(alarm), is(true));

        alarm.setTime(midHour.plusMinutes(Alarm.TOO_SOON_MINUTES));
        assertThat(presenter.isAlarmTooSoon(alarm), is(true));

        alarm.setTime(midHour.plusMinutes(Alarm.TOO_SOON_MINUTES * 2));
        assertThat(presenter.isAlarmTooSoon(alarm), is(false));

        alarm.setTime(midHour.minusMinutes(Alarm.TOO_SOON_MINUTES * 2));
        assertThat(presenter.isAlarmTooSoon(alarm), is(false));


        final LocalTime hourBoundary = new LocalTime(9, 59, 0);

        DateTimeUtils.setCurrentMillisFixed(hourBoundary.toDateTimeToday().getMillis());
        alarm.setTime(hourBoundary);
        assertThat(presenter.isAlarmTooSoon(alarm), is(true));

        alarm.setTime(hourBoundary.plusMinutes(Alarm.TOO_SOON_MINUTES / 2));
        assertThat(presenter.isAlarmTooSoon(alarm), is(true));

        alarm.setTime(hourBoundary.plusMinutes(Alarm.TOO_SOON_MINUTES));
        assertThat(presenter.isAlarmTooSoon(alarm), is(true));

        alarm.setTime(hourBoundary.plusMinutes(Alarm.TOO_SOON_MINUTES * 2));
        assertThat(presenter.isAlarmTooSoon(alarm), is(false));

        alarm.setTime(hourBoundary.minusMinutes(Alarm.TOO_SOON_MINUTES * 2));
        assertThat(presenter.isAlarmTooSoon(alarm), is(false));
    }
}
