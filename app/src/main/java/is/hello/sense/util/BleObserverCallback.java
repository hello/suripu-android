package is.hello.sense.util;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.devices.HelloBleDevice;

import rx.Observer;

public class BleObserverCallback<T> implements BleOperationCallback<T> {
    public static final long NO_TIMEOUT = -1;

    public final Observer<? super T> observer;
    public final Handler timeoutHandler;
    public final Runnable onTimeout;

    public boolean timedOut = false;

    public BleObserverCallback(@NonNull Observer<? super T> observer,
                               @NonNull Handler timeoutHandler,
                               long timeoutMs) {
        this.observer = observer;
        if (timeoutMs == NO_TIMEOUT) {
            this.timeoutHandler = null;
            this.onTimeout = null;
        } else {
            this.timeoutHandler = timeoutHandler;
            this.onTimeout = () -> {
                Logger.info(BleObserverCallback.class.getSimpleName(), "onTimeout");

                this.timedOut = true;
                observer.onError(new BluetoothError(OperationFailReason.TIME_OUT, 2));
            };
            timeoutHandler.postDelayed(onTimeout, timeoutMs);
        }
    }

    @Override
    public void onCompleted(HelloBleDevice sender, T data) {
        if (timedOut) {
            Logger.warn(BleObserverCallback.class.getSimpleName(), "onCompleted called after timeout.");
            return;
        }

        if (timeoutHandler != null)
            timeoutHandler.removeCallbacks(onTimeout);

        observer.onNext(data);
        observer.onCompleted();
    }

    @Override
    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
        if (timedOut) {
            Logger.warn(BleObserverCallback.class.getSimpleName(), "onFailed called after timeout. " + reason);
            return;
        }

        if (timeoutHandler != null)
            timeoutHandler.removeCallbacks(onTimeout);

        observer.onError(new BluetoothError(reason, errorCode));
    }

    public static class BluetoothError extends Exception {
        public final BleOperationCallback.OperationFailReason failureReason;
        public final int errorCode;

        public BluetoothError(@NonNull BleOperationCallback.OperationFailReason failureReason, int errorCode) {
            this.failureReason = failureReason;
            this.errorCode = errorCode;
        }

        @Override
        public String getMessage() {
            return failureReason + ": " + errorCode;
        }
    }
}
