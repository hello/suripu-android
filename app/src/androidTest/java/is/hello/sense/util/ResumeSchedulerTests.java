package is.hello.sense.util;

import junit.framework.TestCase;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class ResumeSchedulerTests extends TestCase {
    private boolean isResumed = false;
    private final ResumeScheduler.Coordinator coordinator = new ResumeScheduler.Coordinator(() -> isResumed);
    private final ResumeScheduler scheduler = new ResumeScheduler(coordinator, Schedulers.immediate());

    public void testResumedBehavior() throws Exception {
        this.isResumed = true;
        coordinator.resume();

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
        coordinator.resume();

        assertTrue(called.get());
    }
}
