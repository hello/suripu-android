package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.EnumSet;
import java.util.List;

import is.hello.sense.bluetooth.stacks.util.ScanCriteria;
import rx.Observable;
import rx.Scheduler;

public class TestBluetoothStack implements BluetoothStack {
    @NonNull
    @Override
    public Observable<List<Peripheral>> discoverPeripherals(@NonNull ScanCriteria scanCriteria) {
        return null;
    }

    @NonNull
    @Override
    public Scheduler getScheduler() {
        return null;
    }

    @Override
    public <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe) {
        return null;
    }

    @Override
    public Observable<Boolean> isEnabled() {
        return null;
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
        return null;
    }

    @Override
    public SupportLevel getDeviceSupportLevel() {
        return null;
    }
}
