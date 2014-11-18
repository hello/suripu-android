package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.errors.BluetoothDisabledError;
import is.hello.sense.bluetooth.errors.BluetoothGattError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.OperationTimeout;
import is.hello.sense.bluetooth.stacks.SchedulerOperationTimeout;
import is.hello.sense.bluetooth.stacks.util.ScanCriteria;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.functional.Functions;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Scheduler;
import rx.subjects.ReplaySubject;

import static rx.android.observables.AndroidObservable.fromBroadcast;

public class AndroidBluetoothStack implements BluetoothStack {
    final @NonNull Context applicationContext;
    final @NonNull Scheduler scheduler;

    final @NonNull BluetoothManager bluetoothManager;
    private final @Nullable BluetoothAdapter adapter;

    final @NonNull ReplaySubject<Boolean> enabled = ReplaySubject.createWithSize(1);

    public AndroidBluetoothStack(@NonNull Context applicationContext, @NonNull Scheduler scheduler) {
        this.applicationContext = applicationContext;
        this.scheduler = scheduler;

        this.bluetoothManager = (BluetoothManager) applicationContext.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = bluetoothManager.getAdapter();
        if (adapter != null) {
            Observable<Intent> stateChanged = fromBroadcast(applicationContext, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            stateChanged.observeOn(getScheduler()).subscribe(ignored -> enabled.onNext(adapter.isEnabled()), Functions.LOG_ERROR);
            enabled.onNext(adapter.isEnabled());
        } else {
            Logger.warn(LOG_TAG, "Host device has no bluetooth hardware!");
            enabled.onNext(false);
        }
    }


    @NonNull BluetoothAdapter getAdapter() {
        if (adapter == null) {
            throw new NullPointerException("Host device has no bluetooth hardware!");
        }

        return adapter;
    }


    @NonNull
    @Override
    public Observable<List<Peripheral>> discoverPeripherals(@NonNull ScanCriteria scanCriteria) {
        if (adapter != null && adapter.isEnabled()) {
            return newConfiguredObservable(new PeripheralScanner(this, scanCriteria));
        } else {
            return Observable.error(new BluetoothDisabledError());
        }
    }

    @NonNull
    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe) {
        return Observable.create(onSubscribe).subscribeOn(getScheduler());
    }

    @Override
    public OperationTimeout acquireOperationTimeout(@NonNull String name, long duration, TimeUnit timeUnit) {
        return SchedulerOperationTimeout.acquire(name, duration, timeUnit);
    }

    @Override
    public Observable<Boolean> isEnabled() {
        return this.enabled;
    }

    @Override
    public boolean errorRequiresReconnect(@Nullable Throwable e) {
        return e != null && (e instanceof OperationTimeoutError || e instanceof BluetoothGattError);
    }

    @Override
    public boolean isErrorFatal(@Nullable Throwable e) {
        return e != null && (e instanceof BluetoothGattError && ((BluetoothGattError) e).statusCode == BluetoothGattError.STACK_ERROR);
    }

    @Override
    public EnumSet<Traits> getTraits() {
        return EnumSet.of(Traits.BONDS_NOT_PERSISTENT);
    }

    @Override
    public SupportLevel getDeviceSupportLevel() {
        return DeviceSupport.getDeviceSupportLevel();
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
