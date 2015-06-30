package is.hello.sense.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static is.hello.sense.AssertExtensions.assertNoThrow;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class StateSafeExecutorTests implements StateSafeExecutor.Resumes {
    private final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(this);

    //region Faux-lifecycle

    private boolean resumed = true;

    @Override
    public boolean isResumed() {
        return resumed;
    }

    void resume() {
        this.resumed = true;
        stateSafeExecutor.executePendingForResume();
    }

    void pause() {
        this.resumed = false;
    }

    @Before
    public void setUp() {
        this.resumed = true;
    }

    @After
    public void tearDown() {
        stateSafeExecutor.clearPending();
    }

    //endregion


    //region Tests

    @Test
    public void resumedBehavior() throws Exception {
        resume();

        TestRunnable runnable1 = new TestRunnable();
        stateSafeExecutor.execute(runnable1);
        assertTrue(runnable1.called);

        TestRunnable runnable2 = new TestRunnable();
        stateSafeExecutor.execute(runnable2);
        assertTrue(runnable2.called);
    }

    @Test
    public void pausedBehavior() throws Exception {
        pause();

        TestRunnable runnable1 = new TestRunnable();
        stateSafeExecutor.execute(runnable1);
        assertFalse(runnable1.called);

        TestRunnable runnable2 = new TestRunnable();
        stateSafeExecutor.execute(runnable2);
        assertFalse(runnable2.called);

        resume();

        assertTrue(runnable1.called);
        assertTrue(runnable2.called);
    }

    @Test
    public void isReentrant() throws Exception {
        pause();

        TestRunnable runnable = new TestRunnable();
        stateSafeExecutor.execute(() -> {
            pause();

            stateSafeExecutor.execute(runnable);
            assertFalse(runnable.called);
        });

        assertNoThrow(this::resume);
        assertFalse(runnable.called);

        resume();
        assertTrue(runnable.called);
    }

    //endregion


    static class TestRunnable implements Runnable {
        boolean called = false;

        @Override
        public void run() {
            this.called = true;
        }
    }
}
