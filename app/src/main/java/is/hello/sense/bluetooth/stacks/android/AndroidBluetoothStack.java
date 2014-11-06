package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.EnumSet;
import java.util.List;

import is.hello.sense.bluetooth.errors.BluetoothDisabledError;
import is.hello.sense.bluetooth.errors.GattError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.DiscoveryCriteria;
import is.hello.sense.bluetooth.stacks.Peripheral;
import rx.Observable;
import rx.Scheduler;

public class AndroidBluetoothStack implements BluetoothStack {
    final @NonNull Context applicationContext;
    final @NonNull Scheduler scheduler;

    final @NonNull BluetoothManager bluetoothManager;
    final @NonNull BluetoothAdapter adapter;

    public AndroidBluetoothStack(@NonNull Context applicationContext, @NonNull Scheduler scheduler) {
        this.applicationContext = applicationContext;
        this.scheduler = scheduler;

        this.bluetoothManager = (BluetoothManager) applicationContext.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = bluetoothManager.getAdapter();
    }

    @NonNull
    @Override
    public Observable<List<Peripheral>> discoverPeripherals(@NonNull DiscoveryCriteria discoveryCriteria) {
        if (adapter.isEnabled()) {
            return newConfiguredObservable(new PeripheralScanner(this, discoveryCriteria));
        } else {
            return Observable.error(new BluetoothDisabledError());
        }
    }

    @Override
    public <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe) {
        return Observable.create(onSubscribe)
                .subscribeOn(scheduler);
    }


    @Override
    public boolean isErrorFatal(@Nullable Throwable e) {
        return e != null && (e instanceof OperationTimeoutError || e instanceof GattError);
    }

    @Override
    public EnumSet<Traits> getTraits() {
        return EnumSet.of(Traits.BONDS_NOT_PERSISTENT);
    }

    @Override
    public SupportLevel getDeviceSupportLevel() {
        return SupportLevel.UNTESTED;
    }


    @Override
    public String toString() {
        return "AndroidBluetoothStack{" +
                "applicationContext=" + applicationContext +
                ", scheduler=" + scheduler +
                ", adapter=" + adapter +
                ", traits=" + getTraits() +
                '}';
    }
}
