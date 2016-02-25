package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.service.SenseService;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.SafeObserverWrapper;
import rx.Observable;
import rx.Subscription;

@Singleton public class SensePresenter extends Presenter {
    public static final int FAILS_BEFORE_HIGH_POWER = 2;

    private final BluetoothStack bluetoothStack;

    private int peripheralNotFoundCount = 0;
    private boolean wantsHighPowerPreScan = false;

    @Nullable
    private WeakReference<Subscription> scanSubscription;
    public final PresenterSubject<GattPeripheral> peripheral = PresenterSubject.create();

    @Inject public SensePresenter(@NonNull BluetoothStack bluetoothStack) {
        this.bluetoothStack = bluetoothStack;
    }

    //region High Power Mode

    public void trackPeripheralNotFound() {
        this.peripheralNotFoundCount++;
    }

    public boolean shouldPromptForHighPowerScan() {
        return (!wantsHighPowerPreScan && peripheralNotFoundCount >= FAILS_BEFORE_HIGH_POWER);
    }

    public void setWantsHighPowerPreScan(boolean wantsHighPowerPreScan) {
        logEvent("setWantsHighPowerPreScan(" + wantsHighPowerPreScan + ")");
        this.wantsHighPowerPreScan = wantsHighPowerPreScan;
    }

    //endregion


    //region Scanning

    private void scan(@NonNull PeripheralCriteria criteria) {
        logEvent("scan(" + criteria + ")");

        final Subscription scanSubscription = Functions.extract(this.scanSubscription);
        if (scanSubscription != null) {
            scanSubscription.unsubscribe();
            this.scanSubscription = null;
        }

        criteria.setWantsHighPowerPreScan(wantsHighPowerPreScan);

        final Observable<List<GattPeripheral>> discoverPeripherals =
                bluetoothStack.discoverPeripherals(criteria);
        final Observable<GattPeripheral> closest = discoverPeripherals.flatMap(peripherals -> {
            if (peripherals.isEmpty()) {
                return Observable.error(new SenseNotFoundError());
            } else {
                return Observable.just(Collections.max(peripherals));
            }
        });

        final Subscription newSubscription = closest.subscribe(new SafeObserverWrapper<>(peripheral));
        this.scanSubscription = new WeakReference<>(newSubscription);
    }

    public void scanForDevice(@NonNull SenseDevice device) {
        final PeripheralCriteria criteria = new PeripheralCriteria();
        SenseService.prepareForScan(criteria, device.deviceId);
        scan(criteria);
    }

    public void scanForClosestSense() {
        final PeripheralCriteria criteria = new PeripheralCriteria();
        SenseService.prepareForScan(criteria, null);
        scan(criteria);
    }

    //endregion
}
