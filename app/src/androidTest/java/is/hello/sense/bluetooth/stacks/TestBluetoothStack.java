package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.stacks.util.ScanCriteria;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import static is.hello.sense.functional.Functions.filteredList;
import static is.hello.sense.functional.Functions.mapList;

public class TestBluetoothStack implements BluetoothStack {
    private final TestBluetoothStackBehavior behavior;

    public TestBluetoothStack(@NonNull TestBluetoothStackBehavior behavior) {
        this.behavior = behavior;
    }

    @SuppressWarnings("RedundantCast")
    @NonNull
    @Override
    public Observable<List<Peripheral>> discoverPeripherals(@NonNull ScanCriteria scanCriteria) {
        return Observable.just(behavior.peripheralsInRange)
                         .map(ps -> filteredList(ps, p -> scanCriteria.matches(p.scanResponse)))
                         .map(ps -> mapList(ps, p -> (Peripheral) new TestPeripheral(this, p)))
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
    public boolean errorRequiresReconnect(@Nullable Throwable e) {
        return false;
    }

    @Override
    public boolean isErrorFatal(@Nullable Throwable e) {
        return false;
    }

    @Override
    public EnumSet<Traits> getTraits() {
        return EnumSet.noneOf(Traits.class);
    }

    @Override
    public SupportLevel getDeviceSupportLevel() {
        return SupportLevel.UNTESTED;
    }
}
