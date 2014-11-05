package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import java.util.List;

import rx.Observable;

public interface BluetoothStack {
    public static final String LOG_TAG = "Bluetooth." + BluetoothStack.class.getSimpleName();

    @NonNull Observable<List<Peripheral>> scanForDevice(@NonNull ScanCriteria scanCriteria);

    <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe);

}
