package is.hello.sense.util;

import android.support.annotation.NonNull;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class ResumeSchedulerTests extends TestCase {
    private final TestResumable resumable = new TestResumable();
    private final ResumeScheduler scheduler = new ResumeScheduler(resumable, Schedulers.immediate());

    public void testResumedBehavior() throws Exception {
        resumable.onResume();

        AtomicBoolean called = new AtomicBoolean(false);
        Action0 action = () -> called.set(true);
        Scheduler.Worker worker = scheduler.createWorker();
        worker.schedule(action);

        assertTrue(called.get());
    }

    public void testPausedBehavior() throws Exception {
        resumable.onPause();

        AtomicBoolean called = new AtomicBoolean(false);
        Action0 action = () -> called.set(true);
        Scheduler.Worker worker = scheduler.createWorker();
        worker.schedule(action);

        assertFalse(called.get());

        resumable.onResume();

        assertTrue(called.get());
    }

    public static class TestResumable implements ResumeScheduler.Coordinator {
        // This is the same code used by SenseActivity and InjectionFragment.
        private final List<Runnable> onResumeRunnables = new ArrayList<>();
        private boolean isResumed = false;

        void onPause() {
            this.isResumed = false;
        }

        void onResume() {
            this.isResumed = true;

            synchronized (onResumeRunnables) {
                for (Runnable runnable : onResumeRunnables) {
                    runnable.run();
                }
                onResumeRunnables.clear();
            }
        }

        @Override
        public void postOnResume(@NonNull Runnable runnable) {
            if (isResumed) {
                runnable.run();
            } else {
                synchronized (onResumeRunnables) {
                    onResumeRunnables.add(runnable);
                }
            }
        }

        @Override
        public void cancelPostOnResume(@NonNull Runnable runnable) {
            synchronized (onResumeRunnables) {
                onResumeRunnables.remove(runnable);
            }
        }
    }

}
