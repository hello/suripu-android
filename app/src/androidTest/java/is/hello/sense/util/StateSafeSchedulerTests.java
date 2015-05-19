package is.hello.sense.util;

import junit.framework.TestCase;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class StateSafeSchedulerTests extends TestCase {
    private boolean isResumed = false;
    private final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(() -> isResumed);
    private final StateSafeScheduler scheduler = new StateSafeScheduler(stateSafeExecutor, Schedulers.immediate());

    public void testResumedBehavior() throws Exception {
        this.isResumed = true;
        stateSafeExecutor.executePendingForResume();

        AtomicBoolean called = new AtomicBoolean(false);
        Action0 action = () -> called.set(true);
        Scheduler.Worker worker = scheduler.createWorker();
        worker.schedule(action);

        assertTrue(called.get());
    }

    public void testPausedBehavior() throws Exception {
        this.isResumed = false;

        AtomicBoolean called = new AtomicBoolean(false);
        Action0 action = () -> called.set(true);
        Scheduler.Worker worker = scheduler.createWorker();
        worker.schedule(action);

        assertFalse(called.get());

        this.isResumed = true;
        stateSafeExecutor.executePendingForResume();

        assertTrue(called.get());
    }
}
