package is.hello.sense.util;

import android.os.Looper;
import android.support.annotation.NonNull;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class ResumeSchedulerTests extends TestCase {

    public void testResumedBehavior() throws Exception {
        fail();
    }

    public void testPausedBehavior() throws Exception {
        fail();
    }

    public static class TestResumable implements ResumeScheduler.Resumable {
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
            assertNotSame(Looper.getMainLooper(), Looper.myLooper());

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
            assertNotSame(Looper.getMainLooper(), Looper.myLooper());

            synchronized (onResumeRunnables) {
                onResumeRunnables.remove(runnable);
            }
        }
    }
    
}
