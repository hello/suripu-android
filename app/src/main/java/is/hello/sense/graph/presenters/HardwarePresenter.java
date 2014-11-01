package is.hello.sense.graph.presenters;

import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;
import com.hello.ble.BleOperationCallback;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Morpheus;
import com.hello.ble.protobuf.MorpheusBle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.util.BleObserverCallback;
import is.hello.sense.util.Constants;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

@Singleton public class HardwarePresenter extends Presenter {
    private final PreferencesPresenter preferencesPresenter;
    private final ApiSessionManager apiSessionManager;
    private final Handler timeoutHandler;

    private @Nullable Observable<Morpheus> repairingTask;
    private @Nullable Morpheus device;

    private final Action1<Throwable> respondToError = e -> {
        if (BleObserverCallback.BluetoothError.isFatal(e)) {
            clearDevice();
        }
    };

    @Inject public HardwarePresenter(@NonNull PreferencesPresenter preferencesPresenter,
                                     @NonNull ApiSessionManager apiSessionManager) {
        this.preferencesPresenter = preferencesPresenter;
        this.apiSessionManager = apiSessionManager;
        this.timeoutHandler = new Handler(Looper.getMainLooper());
    }


    public void setPairedDeviceAddress(@Nullable String address) {
        logEvent("saving paired device address: " + address);

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


    public @Nullable Morpheus getDevice() {
        return device;
    }

    private @NonNull <T> Observable<T> noDeviceError() {
        return Observable.error(new NoPairedDeviceException());
    }

    public Observable<Set<Morpheus>> scanForDevices() {
        logEvent("scanForDevices()");

        return Observable.create((Observable.OnSubscribe<Set<Morpheus>>) s -> Morpheus.discover(new BleObserverCallback<>(s, null, timeoutHandler, BleObserverCallback.NO_TIMEOUT), Constants.BLE_SCAN_TIMEOUT_MS))
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public @Nullable Morpheus bestDeviceForPairing(@NonNull Set<Morpheus> devices) {
        logEvent("bestDeviceForPairing(" + devices + ")");

        if (devices.isEmpty()) {
            return null;
        } else {
            return Collections.max(devices, (l, r) -> Ints.compare(l.getScanTimeRssi(), r.getScanTimeRssi()));
        }
    }

    public Observable<Morpheus> rediscoverDevice() {
        logEvent("rediscoverDevice()");

        if (device != null) {
            logEvent("device already rediscovered " + device);

            return Observable.just(device);
        }

        if (repairingTask != null) {
            return repairingTask;
        }

        String deviceAddress = preferencesPresenter.getString(PreferencesPresenter.PAIRED_DEVICE_ADDRESS, null);
        if (TextUtils.isEmpty(deviceAddress)) {
            return Observable.error(new Exception(""));
        } else {
            this.repairingTask = Observable.create((Observable.OnSubscribe<Morpheus>) s -> Morpheus.discover(deviceAddress, new BleObserverCallback<>(s, null, timeoutHandler, BleObserverCallback.NO_TIMEOUT), Constants.BLE_SCAN_TIMEOUT_MS))
                    .flatMap(device -> {
                        if (device != null) {
                            logEvent("rediscoveredDevice(" + device + ")");
                            this.device = device;
                            this.repairingTask = null;

                            return Observable.just(device);
                        } else {
                            return Observable.error(new Exception("Could not rediscover device."));
                        }
                    })
                    .subscribeOn(AndroidSchedulers.mainThread());
            return repairingTask;
        }
    }

    public Observable<Void> connectToDevice(@NonNull Morpheus device) {
        logEvent("connectToDevice(" + device + ")");

        if (device.isConnected() && device.getBondState() != BluetoothDevice.BOND_NONE) {
            logEvent("already paired with device " + device);

            return Observable.just(null);
        }

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.connect(new BleObserverCallback<>(s, device, timeoutHandler, Constants.BLE_DEFAULT_TIMEOUT_MS)))
                         .doOnNext(ignored -> {
                             logEvent("pairedWithDevice(" + device + ")");
                             setPairedDeviceAddress(device.getAddress());
                             this.device = device;
                         })
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Void> reconnect() {
        logEvent("reconnect()");

        if (device == null) {
            return noDeviceError();
        }

        return Observable.create((Observable.OnSubscribe<Void>) s -> {
            device.setDisconnectedCallback(new BleOperationCallback<Integer>() {
                @Override
                public void onCompleted(HelloBleDevice sender, Integer data) {
                    device.connect(new BleObserverCallback<>(s, device, timeoutHandler, BleObserverCallback.NO_TIMEOUT));
                }

                @Override
                public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                    logEvent("reconnect failed " + reason + " (" + errorCode + ")");
                    s.onError(new BleObserverCallback.BluetoothError(reason, errorCode));
                }
            });
            device.disconnect();
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    public Observable<List<MorpheusBle.wifi_endpoint>> scanForWifiNetworks() {
        if (device == null) {
            return noDeviceError();
        }

        return Observable.create((Observable.OnSubscribe<List<MorpheusBle.wifi_endpoint>>) s -> device.scanSupportedWIFIAP(new BleObserverCallback<>(s, device, timeoutHandler, BleObserverCallback.NO_TIMEOUT)))
                         .subscribeOn(AndroidSchedulers.mainThread())
                         .doOnError(this.respondToError)
                         .map(networks -> {
                             if (!networks.isEmpty())
                                 Collections.sort(networks, (l, r) -> Ints.compare(l.getRssi(), r.getRssi()));

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

        if (device == null) {
            return noDeviceError();
        }

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.setWIFIConnection(bssid, ssid, securityType, password, new BleObserverCallback<>(s, device, timeoutHandler, Constants.BLE_SET_WIFI_TIMEOUT_MS)))
                         .subscribeOn(AndroidSchedulers.mainThread())
                         .doOnError(this.respondToError);
    }

    public Observable<Void> linkAccount() {
        logEvent("linkAccount()");

        if (device == null) {
            return noDeviceError();
        }

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.linkAccount(apiSessionManager.getAccessToken(), new BleObserverCallback<>(s, device, timeoutHandler, Constants.BLE_DEFAULT_TIMEOUT_MS)))
                         .subscribeOn(AndroidSchedulers.mainThread())
                         .doOnError(this.respondToError);
    }

    public Observable<String> linkPill() {
        logEvent("linkPill()");

        if (device == null) {
            return noDeviceError();
        }

        return Observable.create((Observable.OnSubscribe<String>) s -> device.pairPill(apiSessionManager.getAccessToken(), new BleObserverCallback<>(s, device, timeoutHandler, Constants.BLE_DEFAULT_TIMEOUT_MS)))
                         .subscribeOn(AndroidSchedulers.mainThread())
                         .doOnNext(pillId -> {
                             logEvent("linkedWithPill(" + pillId + ")");
                             setPairedPillId(pillId);
                         })
                         .doOnError(this.respondToError);
    }

    public Observable<Void> putIntoPairingMode() {
        logEvent("putIntoPairingMode()");

        if (device == null) {
            return noDeviceError();
        }

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.switchToPairingMode(new BleObserverCallback<>(s, device, timeoutHandler, Constants.BLE_DEFAULT_TIMEOUT_MS)))
                         .subscribeOn(AndroidSchedulers.mainThread())
                         .doOnError(this.respondToError);
    }

    public Observable<Void> factoryReset() {
        logEvent("factoryReset()");

        if (device == null) {
            return noDeviceError();
        }

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.factoryReset(new BleObserverCallback<>(s, device, timeoutHandler, Constants.BLE_DEFAULT_TIMEOUT_MS)))
                         .subscribeOn(AndroidSchedulers.mainThread())
                         .doOnError(this.respondToError);
    }

    public void clearDevice() {
        logEvent("clearDevice()");

        if (device != null) {
            if (device.isConnected()) {
                logEvent("disconnect from paired device");
                device.disconnect();
            }

            this.device = null;
        }
    }


    public static class NoPairedDeviceException extends Exception {
        public NoPairedDeviceException() {
            super("HardwarePresenter device method called without paired device.", new NullPointerException());
        }
    }
}
