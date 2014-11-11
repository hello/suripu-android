package is.hello.sense.graph.presenters;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.model.Device;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.util.ScanCriteria;
import is.hello.sense.functional.Functions;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

@Singleton public class HardwarePresenter extends Presenter {
    private final PreferencesPresenter preferencesPresenter;
    private final ApiSessionManager apiSessionManager;
    private final BluetoothStack bluetoothStack;

    private @Nullable Observable<SensePeripheral> repairingTask;
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
            if (bluetoothStack.doesErrorRequireReconnect(e)) {
                clearPeripheral();
            }
        };

        Observable<Intent> logOutSignal = fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOutSignal.subscribe(this::onUserLoggedOut, Functions.LOG_ERROR);

        this.bluetoothEnabled = bluetoothStack.isEnabled();
        bluetoothEnabled.subscribe(this::onBluetoothEnabledChanged, Functions.LOG_ERROR);
    }

    public void onUserLoggedOut(@NonNull Intent intent) {
        setPairedDeviceAddress(null);
        setPairedPillId(null);
    }

    public void onBluetoothEnabledChanged(boolean enabled) {
        logEvent("onBluetoothEnabledChanged(" + enabled + ")");
        if (!enabled) {
            this.peripheral = null;
        }
    }


    public void setPairedDeviceAddress(@Nullable String address) {
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


    public @Nullable SensePeripheral getPeripheral() {
        return peripheral;
    }

    public boolean isErrorFatal(@Nullable Throwable e) {
        return bluetoothStack.isErrorFatal(e);
    }

    private @NonNull <T> Observable<T> noDeviceError() {
        return Observable.error(new NoConnectedPeripheralException());
    }

    public Observable<List<SensePeripheral>> scanForDevices() {
        logEvent("scanForDevices()");

        return SensePeripheral.discover(bluetoothStack, new ScanCriteria());
    }

    public @Nullable SensePeripheral bestDeviceForPairing(@NonNull List<SensePeripheral> devices) {
        logEvent("bestDeviceForPairing(" + devices + ")");

        if (devices.isEmpty()) {
            return null;
        } else {
            return Collections.max(devices, (l, r) -> Functions.compareInts(l.getScannedRssi(), r.getScannedRssi()));
        }
    }

    public Observable<SensePeripheral> rediscoverPeripheral() {
        logEvent("rediscoverPeripheral()");

        if (peripheral != null) {
            logEvent("device already rediscovered " + peripheral);

            return Observable.just(peripheral);
        }

        if (repairingTask != null) {
            return repairingTask;
        }

        String deviceAddress = preferencesPresenter.getString(PreferencesPresenter.PAIRED_DEVICE_ADDRESS, null);
        if (TextUtils.isEmpty(deviceAddress)) {
            return Observable.error(new Exception(""));
        } else {
            this.repairingTask = SensePeripheral.discover(bluetoothStack, ScanCriteria.forAddress(deviceAddress)).flatMap(devices -> {
                if (!devices.isEmpty()) {
                    this.peripheral = devices.get(0);
                    this.repairingTask = null;

                    logEvent("rediscoveredDevice(" + peripheral + ")");
                    return Observable.just(peripheral);
                } else {
                    return Observable.error(new Exception("Could not rediscover device."));
                }
            });
            return repairingTask;
        }
    }

    public Observable<SensePeripheral> discoverDevice(@NonNull Device device) {
        logEvent("discoverDevice(" + device.getDeviceId() + ")");

        if (TextUtils.isEmpty(device.getDeviceId()) || device.getType() != Device.Type.SENSE)
            throw new IllegalArgumentException("Malformed Sense device " + device);

        if (this.peripheral != null) {
            logEvent("peripheral already discovered " + peripheral);

            return Observable.just(this.peripheral);
        }

        if (repairingTask != null) {
            return repairingTask;
        }

        this.repairingTask = SensePeripheral.rediscover(bluetoothStack, device.getDeviceId()).flatMap(peripheral -> {
            logEvent("rediscoveredDevice(" + peripheral + ")");
            this.peripheral = peripheral;
            this.repairingTask = null;

            return Observable.just(this.peripheral);
        });
        return repairingTask;
    }

    public Observable<HelloPeripheral.ConnectStatus> connectToPeripheral(@NonNull SensePeripheral peripheral) {
        logEvent("connectToPeripheral(" + peripheral + ")");

        if (peripheral.isConnected() && peripheral.getBondStatus() != Peripheral.BOND_BONDED) {
            logEvent("already paired with peripheral " + peripheral);

            return Observable.just(null);
        }

        return peripheral.connect().doOnNext(ignored -> {
            logEvent("pairedWithPeripheral(" + peripheral + ")");
            setPairedDeviceAddress(peripheral.getAddress());
            this.peripheral = peripheral;
        });
    }

    public Observable<List<MorpheusBle.wifi_endpoint>> scanForWifiNetworks() {
        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.scanForWifiNetworks()
                     .subscribeOn(AndroidSchedulers.mainThread())
                     .doOnError(this.respondToError)
                     .map(networks -> {
                         if (!networks.isEmpty())
                             Collections.sort(networks, (l, r) -> Functions.compareInts(l.getRssi(), r.getRssi()));

                         return networks;
                     });
    }

    public Observable<List<MorpheusBle.wifi_endpoint>> scanForWifiNetworks(int passCount) {
        if (passCount == 0)
            throw new IllegalArgumentException("passCount == 0");

        if (passCount == 1)
            return scanForWifiNetworks();

        return Observable.create((Observable.OnSubscribe<List<MorpheusBle.wifi_endpoint>>) s -> {
            Scheduler scheduler = Schedulers.computation();
            Observer<List<MorpheusBle.wifi_endpoint>> observer = new Observer<List<MorpheusBle.wifi_endpoint>>() {
                int pass = 0;
                Map<ByteString, MorpheusBle.wifi_endpoint> accumulator = new HashMap<>();

                @Override
                public void onCompleted() {
                    if (++pass < passCount) {
                        scanForWifiNetworks().subscribeOn(scheduler).subscribe(this);
                    } else {
                        List<MorpheusBle.wifi_endpoint> results = new ArrayList<>();
                        results.addAll(accumulator.values());
                        Collections.sort(results, (l, r) -> l.getSsid().compareTo(r.getSsid()));

                        s.onNext(results);
                        s.onCompleted();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    s.onError(e);
                }

                @Override
                public void onNext(List<MorpheusBle.wifi_endpoint> wifi_endpoints) {
                    for (MorpheusBle.wifi_endpoint endpoint : wifi_endpoints) {
                        accumulator.put(endpoint.getBssid(), endpoint);
                    }
                }
            };
            scanForWifiNetworks().subscribeOn(scheduler).subscribe(observer);
        });
    }

    public Observable<Void> sendWifiCredentials(String bssid, String ssid, MorpheusBle.wifi_endpoint.sec_type securityType, String password) {
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

    public Observable<Void> putIntoPairingMode() {
        logEvent("putIntoPairingMode()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.setPairingModeEnabled(true)
                     .doOnError(this.respondToError);
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


    public static class NoConnectedPeripheralException extends Exception {
        public NoConnectedPeripheralException() {
            super("HardwarePresenter peripheral method called without paired peripheral.", new NullPointerException());
        }
    }
}
