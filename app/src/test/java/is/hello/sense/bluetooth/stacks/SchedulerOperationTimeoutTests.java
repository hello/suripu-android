package is.hello.sense.bluetooth.stacks;

import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.sense.graph.SenseTestCase;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchedulerOperationTimeoutTests extends SenseTestCase {
    private static final Scheduler TEST_SCHEDULER = AndroidSchedulers.mainThread();

    @Test
    public void scheduling() throws Exception {
        OperationTimeout timeout = new SchedulerOperationTimeout("Test", 500, TimeUnit.MILLISECONDS);
        AtomicBoolean called = new AtomicBoolean();
        timeout.setTimeoutAction(() -> called.set(true), TEST_SCHEDULER);

        timeout.schedule();
        timeout.unschedule();
        assertFalse(called.get());

        timeout.schedule();
        Robolectric.flushForegroundThreadScheduler();
        Thread.sleep(800, 0);
        timeout.unschedule();
        assertTrue(called.get());
    }
}
