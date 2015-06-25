package is.hello.sense.graph.presenters;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.Alarm;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTests;
import is.hello.sense.util.Sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
public class SmartAlarmPresenterTests extends InjectionTests {
    @Inject SmartAlarmPresenter presenter;

    @Test
    public void update() throws Exception {
        ArrayList<Alarm> smartAlarms = Sync.wrapAfter(presenter::update, presenter.alarms).last();
        assertEquals(1, smartAlarms.size());
    }

    @Test
    public void validateAlarms() throws Exception {
        Alarm sunday = new Alarm();
        sunday.getDaysOfWeek().add(DateTimeConstants.SUNDAY);
        sunday.setSmart(true);

        Alarm monday = new Alarm();
        monday.getDaysOfWeek().add(DateTimeConstants.MONDAY);
        monday.setSmart(true);

        assertTrue(presenter.validateAlarms(Lists.newArrayList(sunday, monday)));
        assertFalse(presenter.validateAlarms(Lists.newArrayList(sunday, sunday)));
        assertFalse(presenter.validateAlarms(Lists.newArrayList(monday, monday)));

        final Alarm stupidSunday = new Alarm();
        stupidSunday.setSmart(false);
        stupidSunday.getDaysOfWeek().add(DateTimeConstants.SUNDAY);
        assertTrue(presenter.validateAlarms(Lists.newArrayList(sunday, stupidSunday)));

        Alarm disabledSunday = new Alarm();
        disabledSunday.getDaysOfWeek().add(DateTimeConstants.SUNDAY);
        disabledSunday.setSmart(true);
        disabledSunday.setEnabled(false);
        assertTrue(presenter.validateAlarms(Lists.newArrayList(sunday, disabledSunday)));
    }

    @Test
    public void save() throws Exception {
        ArrayList<Alarm> goodAlarms = new ArrayList<>();
        Sync.wrap(presenter.save(goodAlarms))
            .assertNotNull();

        Alarm sunday = new Alarm();
        sunday.getDaysOfWeek().add(DateTimeConstants.SUNDAY);
        sunday.setSmart(true);
        ArrayList<Alarm> badAlarms = Lists.newArrayList(sunday, sunday);

        Sync.wrap(presenter.save(badAlarms))
            .assertThrows(SmartAlarmPresenter.DayOverlapError.class);
    }

    @Test
    public void isTooSoon() throws Exception {
        Alarm alarm = new Alarm();

        LocalTime midHour = new LocalTime(9, 30, 0);
        LocalTime tooSoonMidHour = midHour.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES / 2);
        LocalTime pastCutOffMidHour = midHour.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);
        LocalTime beforeCutOffMidHour = midHour.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);

        alarm.setTime(midHour);
        assertTrue(presenter.isAlarmTooSoon(midHour, alarm));

        alarm.setTime(tooSoonMidHour);
        assertTrue(presenter.isAlarmTooSoon(midHour, alarm));

        alarm.setTime(pastCutOffMidHour);
        assertFalse(presenter.isAlarmTooSoon(midHour, alarm));

        alarm.setTime(beforeCutOffMidHour);
        assertFalse(presenter.isAlarmTooSoon(midHour, alarm));


        LocalTime hourBoundary = new LocalTime(9, 59, 0);
        LocalTime tooSoonBoundary = hourBoundary.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES / 2);
        LocalTime pastCutOffBoundary = hourBoundary.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);
        LocalTime beforeCutOffBoundary = hourBoundary.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);

        alarm.setTime(hourBoundary);
        assertTrue(presenter.isAlarmTooSoon(hourBoundary, alarm));

        alarm.setTime(tooSoonBoundary);
        assertTrue(presenter.isAlarmTooSoon(hourBoundary, alarm));

        alarm.setTime(pastCutOffBoundary);
        assertFalse(presenter.isAlarmTooSoon(hourBoundary, alarm));

        alarm.setTime(beforeCutOffBoundary);
        assertFalse(presenter.isAlarmTooSoon(hourBoundary, alarm));
    }
}
