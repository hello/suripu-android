package is.hello.sense.graph.presenters;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    private static final String SAVED_PERIPHERAL = "peripheral";

    private final BluetoothStack bluetoothStack;
    private final SenseServiceConnection serviceConnection;
    private final PreferencesPresenter preferences;

    private int peripheralNotFoundCount = 0;
    private boolean wantsHighPowerPreScan = false;

    @Nullable
    private WeakReference<Subscription> scanSubscription;
    public final PresenterSubject<GattPeripheral> peripheral = PresenterSubject.create();

    @Inject public SensePresenter(@NonNull BluetoothStack bluetoothStack,
                                  @NonNull SenseServiceConnection serviceConnection,
                                  @NonNull PreferencesPresenter preferences) {
        this.bluetoothStack = bluetoothStack;
        this.serviceConnection = serviceConnection;
        this.preferences = preferences;
    }

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        super.onRestoreState(savedState);

        final Parcelable senseState = savedState.getParcelable(SAVED_PERIPHERAL);
        if (senseState != null && !peripheral.hasValue()) {
            peripheral.onNext(bluetoothStack.restoreState(senseState));
        }
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        final GattPeripheral sense = peripheral.getValue();
        if (sense != null) {
            logEvent("onSaveState()");

            final Bundle savedState = new Bundle();
            savedState.putParcelable(SAVED_PERIPHERAL, sense.saveState());
            return savedState;
        } else {
            return null;
        }
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

    public void scanForLastConnectedSense() {
        final String lastAddress = getLastAddress();
        if (lastAddress != null) {
            scan(SenseService.createSenseCriteria()
                             .addPeripheralAddress(lastAddress)
                             .setLimit(1));
        } else {
            scanForClosestSense();
        }
    }

    public void clearPeripheral() {
        peripheral.forget();
    }

    //endregion


    //region Introspection

    public void setLastAddress(@Nullable String address) {
        preferences.edit()
                   .putString(PreferencesPresenter.CONNECTED_SENSE_ADDRESS, address)
                   .apply();
    }

    @Nullable
    public String getLastAddress() {
        return preferences.getString(PreferencesPresenter.CONNECTED_SENSE_ADDRESS, null);
    }

    public boolean hasPeripheral() {
        return peripheral.hasValue();
    }

    public boolean shouldScan() {
        return (!peripheral.hasValue() && Functions.extract(scanSubscription) == null);
    }

    public boolean isDisconnectIntentForSense(@NonNull Intent intent) {
        return (peripheral.hasValue() &&
                Objects.equals(intent.getStringExtra(GattPeripheral.EXTRA_ADDRESS),
                               peripheral.getValue().getAddress()));
    }

    public Observable<ConnectProgress> connectToPeripheral() {
        return Observable.merge(Observable.combineLatest(serviceConnection.senseService(),
                                                         peripheral.take(1),
                                                         (service, peripheral) -> {
                                                             setLastAddress(peripheral.getAddress());
                                                             return service.connect(peripheral);
                                                         }));
    }

    //endregion
}
