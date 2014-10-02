package is.hello.sense;

import android.support.annotation.NonNull;

import junit.framework.Assert;

public class TestUtils {
    public static void assertThrows(@NonNull ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            return;
        }

        Assert.fail("expected exception, got none");
    }

    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
