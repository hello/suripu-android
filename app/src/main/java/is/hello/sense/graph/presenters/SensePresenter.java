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
import is.hello.commonsense.service.SenseServiceConnection;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.SafeObserverWrapper;
import rx.Observable;
import rx.Subscription;

@Singleton public class SensePresenter extends Presenter {
    public static final int FAILS_BEFORE_HIGH_POWER = 2;

    private final BluetoothStack bluetoothStack;
    private final SenseServiceConnection serviceConnection;

    private int peripheralNotFoundCount = 0;
    private boolean wantsHighPowerPreScan = false;

    @Nullable
    private WeakReference<Subscription> scanSubscription;
    public final PresenterSubject<GattPeripheral> peripheral = PresenterSubject.create();

    @Inject public SensePresenter(@NonNull BluetoothStack bluetoothStack,
                                  @NonNull SenseServiceConnection serviceConnection) {
        this.bluetoothStack = bluetoothStack;
        this.serviceConnection = serviceConnection;
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
        scan(SenseService.createSenseCriteria(device.deviceId));
    }

    public void scanForClosestSense() {
        scan(SenseService.createSenseCriteria());
    }

    public void scanForLastSense() {
        scanForClosestSense();
    }

    //endregion


    //region Introspection

    public boolean hasPeripheral() {
        return peripheral.hasValue();
    }

    @Nullable
    public String getAddress() {
        if (peripheral.hasValue()) {
            return peripheral.getValue().getAddress();
        } else {
            return null;
        }
    }

    public Observable<ConnectProgress> connectToPeripheral() {
        return Observable.merge(Observable.combineLatest(serviceConnection.senseService(),
                                                         peripheral.take(1),
                                                         SenseService::connect));
    }

    //endregion
}
