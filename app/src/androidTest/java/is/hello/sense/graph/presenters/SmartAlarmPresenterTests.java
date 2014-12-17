package is.hello.sense.graph.presenters;

import org.joda.time.DateTimeConstants;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

@SuppressWarnings("ConstantConditions")
public class SmartAlarmPresenterTests extends InjectionTestCase {
    @Inject SmartAlarmPresenter presenter;

    public void testUpdate() throws Exception {
        SyncObserver<ArrayList<SmartAlarm>> smartAlarms = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, presenter.alarms);
        smartAlarms.ignore(1);

        presenter.update();

        smartAlarms.await();

        assertNull(smartAlarms.getError());
        assertEquals(1, smartAlarms.getLast().size());
    }

    public void testValidateAlarms() throws Exception {
        SmartAlarm sunday = new SmartAlarm();
        sunday.getDaysOfWeek().add(DateTimeConstants.SUNDAY);

        SmartAlarm monday = new SmartAlarm();
        monday.getDaysOfWeek().add(DateTimeConstants.MONDAY);

        assertTrue(presenter.validateAlarms(Lists.newArrayList(sunday, monday)));
        assertFalse(presenter.validateAlarms(Lists.newArrayList(sunday, sunday)));
        assertFalse(presenter.validateAlarms(Lists.newArrayList(monday, monday)));
    }

    public void testSave() throws Exception {
        ArrayList<SmartAlarm> goodAlarms = new ArrayList<>();
        SyncObserver<VoidResponse> good = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, presenter.save(goodAlarms));
        good.await();

        assertNull(good.getError());
        assertNotNull(good.getSingle());


        // --- //


        SmartAlarm sunday = new SmartAlarm();
        sunday.getDaysOfWeek().add(DateTimeConstants.SUNDAY);
        ArrayList<SmartAlarm> badAlarms = Lists.newArrayList(sunday, sunday);

        SyncObserver<VoidResponse> bad = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, presenter.save(badAlarms));
        bad.await();

        assertNotNull(bad.getError());
        assertNull(bad.getSingle());
    }
}
