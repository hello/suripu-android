package is.hello.sense.graph.presenters;

import org.joda.time.DateTimeConstants;

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
}
