package is.hello.sense.bluetooth.stacks;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import static is.hello.sense.AssertExtensions.assertThrows;

public class SchedulerOperationTimeoutTests extends TestCase {
    private static final Scheduler TEST_SCHEDULER = AndroidSchedulers.mainThread();

    public void testScheduling() throws Exception {
        OperationTimeout timeout = new SchedulerOperationTimeout("Test", 500, TimeUnit.MILLISECONDS);
        AtomicBoolean called = new AtomicBoolean();
        timeout.setTimeoutAction(() -> called.set(true), TEST_SCHEDULER);

        timeout.schedule();
        timeout.unschedule();
        assertFalse(called.get());

        timeout.schedule();
        Thread.sleep(800, 0);
        timeout.unschedule();
        assertTrue(called.get());
    }
}
