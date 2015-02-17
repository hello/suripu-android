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
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.ReplaySubject;

import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

@Singleton public class HardwarePresenter extends Presenter {
    public static final String ACTION_CONNECTION_LOST = HardwarePresenter.class.getName() + ".ACTION_CONNECTION_LOST";

    private final PreferencesPresenter preferencesPresenter;
    private final ApiSessionManager apiSessionManager;
    private final BluetoothStack bluetoothStack;

    private @Nullable ReplaySubject<SensePeripheral> pendingDiscovery;
    private @Nullable ReplaySubject<SensePeripheral.ConnectStatus> connectingToPeripheral;
    private @Nullable SensePeripheral peripheral;

    private final Action1<Throwable> respondToError;

    public final Observable<Boolean> bluetoothEnabled;

    @Inject public HardwarePresenter(@NonNull Context context,
                                     @NonNull PreferencesPresenter preferencesPresenter,
                                     @NonNull ApiSessionManager apiSessionManager,
                                     @NonNull BluetoothStack bluetoothStack) {
        this.preferencesPresenter = preferencesPresenter;
        this.apiSessionManager = apiSessionManager;
        this.bluetoothStack = bluetoothStack;
        this.respondToError = e -> {
            if (bluetoothStack.errorRequiresReconnect(e)) {
                clearPeripheral();
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_CONNECTION_LOST));
            } else if (e != null && e instanceof PeripheralConnectionError) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_CONNECTION_LOST));
            }
        };

        Observable<Intent> logOutSignal = fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOutSignal.subscribe(this::onUserLoggedOut, Functions.LOG_ERROR);

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


    public void setPeripheral(@Nullable SensePeripheral peripheral) {
        this.peripheral = peripheral;
    }

    public boolean hasPeripheral() {
        return (peripheral != null);
    }

    public boolean isConnected() {
        return (peripheral != null && peripheral.isConnected());
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

        if (pendingDiscovery != null) {
            return pendingDiscovery;
        }

        Observable<SensePeripheral> discovery = SensePeripheral.discover(bluetoothStack, new PeripheralCriteria()).flatMap(peripherals -> {
            if (!peripherals.isEmpty()) {
                SensePeripheral closestPeripheral = getClosestPeripheral(peripherals);

                this.peripheral = closestPeripheral;
                this.pendingDiscovery = null;

                return Observable.just(closestPeripheral);
            } else {
                this.peripheral = null;
                this.pendingDiscovery = null;

                return Observable.error(new PeripheralNotFoundError());
            }
        });

        this.pendingDiscovery = ReplaySubject.createWithSize(1);
        discovery.subscribe(this.pendingDiscovery);
        return this.pendingDiscovery;
    }

    public Observable<SensePeripheral> rediscoverLastPeripheral() {
        logEvent("rediscoverLastPeripheral()");

        if (peripheral != null) {
            logEvent("peripheral already rediscovered " + peripheral);

            return Observable.just(peripheral);
        }

        if (pendingDiscovery != null) {
            return pendingDiscovery;
        }

        String address = preferencesPresenter.getString(PreferencesPresenter.PAIRED_DEVICE_ADDRESS, null);
        if (TextUtils.isEmpty(address)) {
            return closestPeripheral();
        } else {
            Observable<SensePeripheral> rediscovery = SensePeripheral.discover(bluetoothStack, PeripheralCriteria.forAddress(address)).flatMap(peripherals -> {
                this.pendingDiscovery = null;

                if (!peripherals.isEmpty()) {
                    this.peripheral = peripherals.get(0);
                    logEvent("rediscoveredDevice(" + peripheral + ")");

                    return Observable.just(peripheral);
                } else {
                    return Observable.error(new PeripheralNotFoundError());
                }
            });

            this.pendingDiscovery = ReplaySubject.createWithSize(1);
            rediscovery.subscribe(this.pendingDiscovery);
            return this.pendingDiscovery;
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

        if (this.pendingDiscovery != null) {
            return pendingDiscovery;
        }

        Observable<SensePeripheral> rediscovery = SensePeripheral.rediscover(bluetoothStack, device.getDeviceId()).flatMap(peripheral -> {
            logEvent("rediscoveredPeripheralForDevice(" + peripheral + ")");
            this.peripheral = peripheral;
            this.pendingDiscovery = null;

            return Observable.just(this.peripheral);
        }).doOnError(ignored -> {
            this.pendingDiscovery = null;
        });


        this.pendingDiscovery = ReplaySubject.createWithSize(1);
        rediscovery.subscribe(this.pendingDiscovery);
        return this.pendingDiscovery;
    }

    public Observable<HelloPeripheral.ConnectStatus> connectToPeripheral() {
        logEvent("connectToPeripheral(" + peripheral + ")");

        if (connectingToPeripheral != null) {
            return connectingToPeripheral;
        }

        if (peripheral == null) {
            return noDeviceError();
        }

        if (peripheral.isConnected() && peripheral.getBondStatus() != Peripheral.BOND_BONDED) {
            logEvent("already paired with peripheral " + peripheral);

            return Observable.just(HelloPeripheral.ConnectStatus.CONNECTED);
        }

        Observable<SensePeripheral.ConnectStatus> connecting = peripheral.connect().doOnCompleted(() -> {
            logEvent("pairedWithPeripheral(" + peripheral + ")");
            setLastPeripheralAddress(peripheral.getAddress());

            this.connectingToPeripheral = null;
        }).doOnError(e -> {
            logEvent("failed to pair with peripheral " + peripheral + ": " + e);
            this.connectingToPeripheral = null;
        });

        this.connectingToPeripheral = ReplaySubject.createWithSize(1);
        connecting.subscribe(connectingToPeripheral);
        return this.connectingToPeripheral;
    }

    public Observable<Void> runLedAnimation(@NonNull SensePeripheral.LedAnimation animationType) {
        logEvent("runLedAnimation()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.runLedAnimation(animationType)
                         .doOnError(this.respondToError);
    }

    public Observable<List<SenseCommandProtos.wifi_endpoint>> scanForWifiNetworks() {
        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.scanForWifiNetworks()
                         .subscribeOn(AndroidSchedulers.mainThread())
                         .doOnError(this.respondToError)
                         .map(networks -> {
                             if (!networks.isEmpty()) {
                                 Collections.sort(networks, (l, r) -> Functions.compareInts(r.getRssi(), l.getRssi()));
                             }

                             return networks;
                         });
    }

    public Observable<SensePeripheral.SenseWifiNetwork> currentWifiNetwork() {
        logEvent("currentWifiNetwork()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.getWifiNetwork()
                         .doOnError(this.respondToError);
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
        static @Nullable SensePeripheral getPeripheral(@NonNull HardwarePresenter presenter) {
            return presenter.peripheral;
        }
    }
}
