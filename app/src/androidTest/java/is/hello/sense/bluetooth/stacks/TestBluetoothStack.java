package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.errors.BluetoothGattError;
import is.hello.sense.bluetooth.errors.BluetoothPowerChangeError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import static is.hello.sense.functional.Lists.filtered;
import static is.hello.sense.functional.Lists.map;

public class TestBluetoothStack implements BluetoothStack {
    private final TestBluetoothStackBehavior behavior;

    public TestBluetoothStack(@NonNull TestBluetoothStackBehavior behavior) {
        this.behavior = behavior;
    }

    @SuppressWarnings("RedundantCast")
    @NonNull
    @Override
    public Observable<List<Peripheral>> discoverPeripherals(@NonNull PeripheralCriteria peripheralCriteria) {
        return Observable.just(behavior.peripheralsInRange)
                         .map(ps -> filtered(ps, p -> peripheralCriteria.matches(p.advertisingData)))
                         .map(ps -> map(ps, p -> (Peripheral) new TestPeripheral(this, p)))
                         .delay(behavior.latency, TimeUnit.SECONDS);
    }

    @NonNull
    @Override
    public Scheduler getScheduler() {
        return AndroidSchedulers.mainThread();
    }

    @Override
    public <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe) {
        return Observable.create(onSubscribe).subscribeOn(getScheduler());
    }

    @Override
    public Observable<Boolean> isEnabled() {
        return behavior.enabled;
    }

    @Override
    public Observable<Void> turnOn() {
        return Observable.error(new BluetoothPowerChangeError());
    }

    @Override
    public Observable<Void> turnOff() {
        return Observable.error(new BluetoothPowerChangeError());
    }

    @Override
    public boolean errorRequiresReconnect(@Nullable Throwable e) {
        return (e != null && (e instanceof OperationTimeoutError ||
                e instanceof BluetoothGattError ||
                e instanceof PeripheralBondAlterationError));
    }

    @Override
    public @Peripheral.Config int getDefaultConfig() {
        return (Peripheral.CONFIG_WAIT_AFTER_SERVICE_DISCOVERY);
    }

    @Override
    public SupportLevel getDeviceSupportLevel() {
        return SupportLevel.UNTESTED;
    }
}
