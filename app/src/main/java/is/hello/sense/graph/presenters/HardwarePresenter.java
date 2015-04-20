package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.model.Device;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.bluetooth.errors.PeripheralConnectionError;
import is.hello.sense.bluetooth.errors.PeripheralNotFoundError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PendingObservables;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static rx.android.content.ContentObservable.fromLocalBroadcast;

@Singleton public class HardwarePresenter extends Presenter {
    public static final String ACTION_CONNECTION_LOST = HardwarePresenter.class.getName() + ".ACTION_CONNECTION_LOST";
    public static final int FAILS_BEFORE_HIGH_POWER = 2;

    private static final String TOKEN_DISCOVERY = HardwarePresenter.class.getSimpleName() + ".TOKEN_DISCOVERY";
    private static final String TOKEN_CONNECT = HardwarePresenter.class.getSimpleName() + ".TOKEN_CONNECT";
    private static final String TOKEN_GET_WIFI = HardwarePresenter.class.getSimpleName() + ".TOKEN_GET_WIFI";

    private final Context context;
    private final PreferencesPresenter preferencesPresenter;
    private final ApiSessionManager apiSessionManager;
    private final BluetoothStack bluetoothStack;

    private final PendingObservables<String> pending = new PendingObservables<>();

    private @Nullable SensePeripheral peripheral;

    private int peripheralNotFoundCount = 0;
    private boolean wantsHighPowerPreScan;

    private final Action1<Throwable> respondToError;

    public final Observable<Boolean> bluetoothEnabled;

    @Inject public HardwarePresenter(@NonNull Context context,
                                     @NonNull PreferencesPresenter preferencesPresenter,
                                     @NonNull ApiSessionManager apiSessionManager,
                                     @NonNull BluetoothStack bluetoothStack) {
        this.context = context;
        this.preferencesPresenter = preferencesPresenter;
        this.apiSessionManager = apiSessionManager;
        this.bluetoothStack = bluetoothStack;
        this.respondToError = e -> {
            if (bluetoothStack.errorRequiresReconnect(e)) {
                clearPeripheral();
            } else if (e != null && e instanceof PeripheralConnectionError) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_CONNECTION_LOST));
            }
        };

        Observable<Intent> logOutSignal = fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOutSignal.subscribe(this::onUserLoggedOut, Functions.LOG_ERROR);

        Observable<Intent> disconnectSignal = fromLocalBroadcast(context, new IntentFilter(Peripheral.ACTION_DISCONNECTED));
        disconnectSignal.subscribe(this::onPeripheralDisconnected, Functions.LOG_ERROR);

        this.bluetoothEnabled = bluetoothStack.isEnabled();
        bluetoothEnabled.subscribe(this::onBluetoothEnabledChanged, Functions.LOG_ERROR);
    }

    public void onUserLoggedOut(@NonNull Intent intent) {
        setLastPeripheralAddress(null);
        setPairedPillId(null);
    }

    public void onBluetoothEnabledChanged(boolean enabled) {
        logEvent("onBluetoothEnabledChanged(" + enabled + ")");
        if (!enabled) {
            this.peripheral = null;
        }
    }

    public void onPeripheralDisconnected(@NonNull Intent intent) {
        if (peripheral != null) {
            String currentAddress = peripheral.getAddress();
            String intentAddress = intent.getStringExtra(Peripheral.EXTRA_ADDRESS);
            if (TextUtils.equals(currentAddress, intentAddress)) {
                logEvent("broadcasting disconnect");

                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_CONNECTION_LOST));
            }
        }
    }


    public void setLastPeripheralAddress(@Nullable String address) {
        logEvent("saving paired peripheral address: " + address);

        SharedPreferences.Editor editor = preferencesPresenter.edit();
        if (address != null) {
            editor.putString(PreferencesPresenter.PAIRED_DEVICE_ADDRESS, address);
        } else {
            editor.remove(PreferencesPresenter.PAIRED_DEVICE_ADDRESS);
        }
        editor.apply();
    }

    public void setPairedPillId(@Nullable String pillId) {
        logEvent("saving paired pill id: " + pillId);

        SharedPreferences.Editor editor = preferencesPresenter.edit();
        if (pillId != null) {
            editor.putString(PreferencesPresenter.PAIRED_PILL_ID, pillId);
        } else {
            editor.remove(PreferencesPresenter.PAIRED_PILL_ID);
        }
        editor.apply();
    }


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

    public void setPeripheral(@Nullable SensePeripheral peripheral) {
        this.peripheral = peripheral;
    }

    public boolean hasPeripheral() {
        return (peripheral != null);
    }

    public boolean isConnected() {
        return (peripheral != null && peripheral.isConnected());
    }

    public @Nullable String getDeviceId() {
        if (peripheral != null) {
            return peripheral.getDeviceId();
        } else {
            return null;
        }
    }

    public BluetoothStack.SupportLevel getDeviceSupportLevel() {
        return bluetoothStack.getDeviceSupportLevel();
    }

    private @NonNull <T> Observable<T> noDeviceError() {
        return Observable.error(new NoConnectedPeripheralException());
    }

    public Observable<Void> turnOnBluetooth() {
        return bluetoothStack.turnOn();
    }

    public Observable<Void> turnOffBluetooth() {
        return bluetoothStack.turnOff();
    }

    public @Nullable SensePeripheral getClosestPeripheral(@NonNull List<SensePeripheral> peripherals) {
        logEvent("getClosestPeripheral(" + peripherals + ")");

        if (peripherals.isEmpty()) {
            return null;
        } else {
            return Collections.max(peripherals, (l, r) -> Functions.compareInts(l.getScannedRssi(), r.getScannedRssi()));
        }
    }

    public Observable<SensePeripheral> closestPeripheral() {
        logEvent("closestPeripheral()");

        if (peripheral != null) {
            logEvent("peripheral already rediscovered " + peripheral);

            return Observable.just(peripheral);
        }

        return pending.bind(TOKEN_DISCOVERY, () -> {
            PeripheralCriteria criteria = new PeripheralCriteria();
            criteria.setWantsHighPowerPreScan(wantsHighPowerPreScan);
            return SensePeripheral.discover(bluetoothStack, criteria)
                    .flatMap(peripherals -> {
                        if (!peripherals.isEmpty()) {
                            SensePeripheral closestPeripheral = getClosestPeripheral(peripherals);
                            this.peripheral = closestPeripheral;
                            return Observable.just(closestPeripheral);
                        } else {
                            this.peripheral = null;
                            return Observable.error(new PeripheralNotFoundError());
                        }
                    });
        });
    }

    public Observable<SensePeripheral> rediscoverLastPeripheral() {
        logEvent("rediscoverLastPeripheral()");

        if (peripheral != null) {
            logEvent("peripheral already rediscovered " + peripheral);

            return Observable.just(peripheral);
        }

        String address = preferencesPresenter.getString(PreferencesPresenter.PAIRED_DEVICE_ADDRESS, null);
        if (TextUtils.isEmpty(address)) {
            return closestPeripheral();
        } else {
            return pending.bind(TOKEN_DISCOVERY, () -> {
                PeripheralCriteria criteria = PeripheralCriteria.forAddress(address);
                criteria.setWantsHighPowerPreScan(wantsHighPowerPreScan);
                return SensePeripheral.discover(bluetoothStack, criteria)
                        .flatMap(peripherals -> {
                            if (!peripherals.isEmpty()) {
                                this.peripheral = peripherals.get(0);
                                logEvent("rediscoveredDevice(" + peripheral + ")");

                                return Observable.just(peripheral);
                            } else {
                                return Observable.error(new PeripheralNotFoundError());
                            }
                        });
            });
        }
    }

    public Observable<SensePeripheral> discoverPeripheralForDevice(@NonNull Device device) {
        logEvent("discoverPeripheralForDevice(" + device.getDeviceId() + ")");

        if (TextUtils.isEmpty(device.getDeviceId()) || device.getType() != Device.Type.SENSE)
            throw new IllegalArgumentException("Malformed Sense device " + device);

        if (this.peripheral != null) {
            logEvent("peripheral already discovered " + peripheral);

            return Observable.just(this.peripheral);
        }

        return pending.bind(TOKEN_DISCOVERY, () -> {
            return SensePeripheral.rediscover(bluetoothStack, device.getDeviceId(), wantsHighPowerPreScan)
                    .flatMap(peripheral -> {
                        logEvent("rediscoveredPeripheralForDevice(" + peripheral + ")");
                        this.peripheral = peripheral;
                        return Observable.just(this.peripheral);
                    });
        });
    }

    public Observable<HelloPeripheral.ConnectStatus> connectToPeripheral() {
        logEvent("connectToPeripheral(" + peripheral + ")");

        if (peripheral == null) {
            return noDeviceError();
        }

        if (peripheral.isConnected() && peripheral.getBondStatus() != Peripheral.BOND_BONDED) {
            logEvent("already paired with peripheral " + peripheral);

            return Observable.just(HelloPeripheral.ConnectStatus.CONNECTED);
        }

        return pending.bind(TOKEN_CONNECT, () -> {
            return peripheral.connect().doOnCompleted(() -> {
                logEvent("pairedWithPeripheral(" + peripheral + ")");
                setLastPeripheralAddress(peripheral.getAddress());
            }).doOnError(e -> {
                logEvent("failed to pair with peripheral " + peripheral + ": " + e);
            });
        });
    }

    public Observable<Void> runLedAnimation(@NonNull SensePeripheral.LedAnimation animationType) {
        logEvent("runLedAnimation()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.runLedAnimation(animationType)
                         .doOnError(this.respondToError);
    }

    private void sortWifiNetworks(@NonNull List<SenseCommandProtos.wifi_endpoint> networks) {
        if (!networks.isEmpty()) {
            Collections.sort(networks, (l, r) -> Functions.compareInts(r.getRssi(), l.getRssi()));
        }
    }

    public Observable<List<SenseCommandProtos.wifi_endpoint>> scanForWifiNetworks() {
        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.scanForWifiNetworks()
                         .subscribeOn(AndroidSchedulers.mainThread())
                         .doOnError(this.respondToError)
                         .map(networks -> {
                             sortWifiNetworks(networks);

                             return networks;
                         });
    }

    public Observable<SensePeripheral.SenseWifiNetwork> currentWifiNetwork() {
        logEvent("currentWifiNetwork()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return pending.bind(TOKEN_GET_WIFI, () -> peripheral.getWifiNetwork()
                                                            .doOnError(this.respondToError));
    }

    public Observable<Void> sendWifiCredentials(@NonNull String bssid,
                                                @NonNull String ssid,
                                                @NonNull SenseCommandProtos.wifi_endpoint.sec_type securityType,
                                                @NonNull String password) {
        logEvent("sendWifiCredentials()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.setWifiNetwork(bssid, ssid, securityType, password)
                         .subscribeOn(AndroidSchedulers.mainThread())
                         .doOnError(this.respondToError);
    }

    public Observable<Void> linkAccount() {
        logEvent("linkAccount()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.linkAccount(apiSessionManager.getAccessToken())
                         .doOnError(this.respondToError);
    }

    public Observable<String> linkPill() {
        logEvent("linkPill()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.pairPill(apiSessionManager.getAccessToken())
                         .doOnNext(pillId -> {
                             logEvent("linkedWithPill(" + pillId + ")");
                             setPairedPillId(pillId);
                         })
                         .doOnError(this.respondToError);
    }

    public Observable<Void> pushData() {
        logEvent("pushData()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.pushData()
                         .doOnError(this.respondToError);
    }

    public Observable<Void> putIntoPairingMode() {
        logEvent("putIntoPairingMode()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.setPairingModeEnabled(true)
                         .doOnError(this.respondToError)
                         .doOnCompleted(this::clearPeripheral);
    }

    public Observable<Void> factoryReset() {
        logEvent("factoryReset()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.factoryReset()
                         .doOnError(this.respondToError);
    }

    public void clearPeripheral() {
        logEvent("clearPeripheral()");

        if (peripheral != null) {
            if (peripheral.isConnected()) {
                logEvent("disconnect from paired peripheral");

                peripheral.disconnect().subscribe(ignored -> logEvent("disconnected peripheral"),
                                                  e -> logEvent("Could not disconnect peripheral " + e));
            }

            this.peripheral = null;
        }
    }


    public static class NoConnectedPeripheralException extends BluetoothError {
        public NoConnectedPeripheralException() {
            super("HardwarePresenter peripheral method called without paired peripheral.", new NullPointerException());
        }
    }

    static final class Tests {
        static void setPeripheral(@NonNull HardwarePresenter presenter, @Nullable SensePeripheral peripheral) {
            presenter.peripheral = peripheral;
        }

        static @Nullable SensePeripheral getPeripheral(@NonNull HardwarePresenter presenter) {
            return presenter.peripheral;
        }

        static void sortWifiNetworks(@NonNull HardwarePresenter presenter, @NonNull List<SenseCommandProtos.wifi_endpoint> endpoints) {
            presenter.sortWifiNetworks(endpoints);
        }
    }
}
