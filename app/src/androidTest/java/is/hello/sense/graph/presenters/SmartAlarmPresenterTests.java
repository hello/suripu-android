package is.hello.sense.graph.presenters;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.Alarm;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

@SuppressWarnings("ConstantConditions")
public class SmartAlarmPresenterTests extends InjectionTestCase {
    @Inject SmartAlarmPresenter presenter;

    public void testUpdate() throws Exception {
        ArrayList<Alarm> smartAlarms = Sync.wrapAfter(presenter::update, presenter.alarms).last();
        assertEquals(1, smartAlarms.size());
    }

    public void testValidateAlarms() throws Exception {
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

    public void testSave() throws Exception {
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

    public void testIsTooSoon() throws Exception {
        Alarm alarm = new Alarm();

        LocalTime now = LocalTime.now(DateTimeZone.getDefault());
        LocalTime tooSoon = now.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES / 2);
        LocalTime pastCutOff = now.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);
        LocalTime beforeCutOff = now.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);

        alarm.setTime(now);
        assertTrue(alarm.isTooSoon());

        alarm.setTime(tooSoon);
        assertTrue(alarm.isTooSoon());

        alarm.setTime(pastCutOff);
        assertFalse(alarm.isTooSoon());

        alarm.setTime(beforeCutOff);
        assertFalse(alarm.isTooSoon());
    }
}
