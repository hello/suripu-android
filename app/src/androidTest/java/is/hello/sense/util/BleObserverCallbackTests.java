package is.hello.sense.util;

import android.os.Handler;
import android.os.Looper;
import android.test.FlakyTest;

import junit.framework.TestCase;

import rx.Observer;

public class BleObserverCallbackTests extends TestCase {
    // Handler.postDelayed calls getPostMessage(Runnable) which
    // calls Message.obtain() which returns a recycled Message
    // with all of its fields nulled/zeroed out. So that means
    // that the `what` for observer callback posts is `0`.

    @FlakyTest
    public void testNonTimeout() throws Exception {
        RecordingObserver<Void> observer = new RecordingObserver<>();
        Handler handler = new Handler(Looper.getMainLooper());
        BleObserverCallback<Void> observerCallback = new BleObserverCallback<>(observer, handler, 5);
        assertTrue(handler.hasMessages(0));

        observerCallback.onCompleted(null, null);

        Thread.sleep(10, 0);

        assertTrue(observer.onNextCalled);
        assertTrue(observer.onCompletedCalled);
        assertFalse(observer.onErrorCalled);
        assertFalse(observerCallback.timedOut);
    }

    @FlakyTest
    public void testTimeout() throws Exception {
        RecordingObserver<Void> observer = new RecordingObserver<>();
        Handler handler = new Handler(Looper.getMainLooper());
        BleObserverCallback<Void> observerCallback = new BleObserverCallback<>(observer, handler, 1);
        assertTrue(handler.hasMessages(0));

        Thread.sleep(10, 0);

        observerCallback.onCompleted(null, null);

        assertFalse(observer.onNextCalled);
        assertFalse(observer.onCompletedCalled);
        assertTrue(observer.onErrorCalled);
        assertTrue(observerCallback.timedOut);
    }


    private static class RecordingObserver<T> implements Observer<T> {
        public boolean onCompletedCalled = false;
        public boolean onErrorCalled = false;
        public boolean onNextCalled = false;

        @Override
        public void onCompleted() {
            this.onCompletedCalled = true;
        }

        @Override
        public void onError(Throwable e) {
            this.onErrorCalled = true;
        }

        @Override
        public void onNext(T t) {
            this.onNextCalled = true;
        }
    }
}
