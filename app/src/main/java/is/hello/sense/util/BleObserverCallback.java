package is.hello.sense.util;

import android.support.annotation.NonNull;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.devices.HelloBleDevice;

import rx.Observer;

public class BleObserverCallback<T> implements BleOperationCallback<T> {
    public final Observer<? super T> observer;

    public BleObserverCallback(Observer<? super T> observer) {
        this.observer = observer;
    }

    @Override
    public void onCompleted(HelloBleDevice sender, T data) {
        observer.onNext(data);
    }

    @Override
    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
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
