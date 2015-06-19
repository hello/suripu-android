package is.hello.sense.util;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StateSafeSchedulerTests {
    private boolean isResumed = false;
    private final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(() -> isResumed);
    private final StateSafeScheduler scheduler = new StateSafeScheduler(stateSafeExecutor, Schedulers.immediate());

    @Test
    public void resumedBehavior() throws Exception {
        this.isResumed = true;
        stateSafeExecutor.executePendingForResume();

        AtomicBoolean called = new AtomicBoolean(false);
        Action0 action = () -> called.set(true);
        Scheduler.Worker worker = scheduler.createWorker();
        worker.schedule(action);

        assertTrue(called.get());
    }

    @Test
    public void pausedBehavior() throws Exception {
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
