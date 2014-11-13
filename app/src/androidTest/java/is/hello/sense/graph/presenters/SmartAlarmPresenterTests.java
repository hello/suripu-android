package is.hello.sense.graph.presenters;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

@SuppressWarnings("ConstantConditions")
public class SmartAlarmPresenterTests extends InjectionTestCase {
    @Inject SmartAlarmPresenter smartAlarmPresenter;

    public void testUpdate() throws Exception {
        SyncObserver<List<SmartAlarm>> smartAlarms = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, smartAlarmPresenter.alarms);
        smartAlarms.ignore(1);

        smartAlarmPresenter.update();

        smartAlarms.await();

        assertNull(smartAlarms.getError());
        assertEquals(1, smartAlarms.getLast().size());
    }

    public void testSave() throws Exception {
        List<SmartAlarm> alarms = Collections.emptyList();
        SyncObserver<VoidResponse> saveAlarms = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, smartAlarmPresenter.save(alarms));
        saveAlarms.await();

        assertNull(saveAlarms.getError());
        assertNotNull(saveAlarms.getSingle());
    }
}
