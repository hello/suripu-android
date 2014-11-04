package is.hello.sense.hardware.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import is.hello.sense.hardware.Device;
import is.hello.sense.hardware.DeviceCenter;
import rx.Observable;
import rx.Scheduler;

public class NativeDeviceCenter implements DeviceCenter {
    final @NonNull Context applicationContext;
    final @NonNull Scheduler scheduler;

    final @NonNull BluetoothManager bluetoothManager;
    final @NonNull BluetoothAdapter adapter;

    public NativeDeviceCenter(@NonNull Context applicationContext, @NonNull Scheduler scheduler) {
        this.applicationContext = applicationContext;
        this.scheduler = scheduler;

        this.bluetoothManager = (BluetoothManager) applicationContext.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = bluetoothManager.getAdapter();
    }


    <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe) {
        return Observable.create(onSubscribe)
                         .subscribeOn(scheduler);
    }


    @NonNull
    @Override
    public Observable<List<Device>> scanForDevice(@NonNull ScanCriteria scanCriteria, long timeoutMs) {
        return newConfiguredObservable(new NativeScanner(this, scanCriteria, timeoutMs));
    }
}
