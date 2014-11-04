package is.hello.sense.graph.presenters;

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

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.devices.SenseDevice;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseBle;
import is.hello.sense.bluetooth.errors.SenseException;
import is.hello.sense.bluetooth.stacks.DeviceCenter;
import is.hello.sense.functional.Functions;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

@Singleton public class HardwarePresenter extends Presenter {
    private final PreferencesPresenter preferencesPresenter;
    private final ApiSessionManager apiSessionManager;
    private final DeviceCenter deviceCenter;

    private @Nullable Observable<SenseDevice> repairingTask;
    private @Nullable SenseDevice device;

    private final Action1<Throwable> respondToError = e -> {
        if (!(e instanceof SenseException)) {
            clearDevice();
        }
    };

    @Inject public HardwarePresenter(@NonNull PreferencesPresenter preferencesPresenter,
                                     @NonNull ApiSessionManager apiSessionManager,
                                     @NonNull DeviceCenter deviceCenter) {
        this.preferencesPresenter = preferencesPresenter;
        this.apiSessionManager = apiSessionManager;
        this.deviceCenter = deviceCenter;
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


    public @Nullable SenseDevice getDevice() {
        return device;
    }

    private @NonNull <T> Observable<T> noDeviceError() {
        return Observable.error(new NoPairedDeviceException());
    }

    public Observable<List<SenseDevice>> scanForDevices() {
        logEvent("scanForDevices()");

        DeviceCenter.ScanCriteria scanCriteria = new DeviceCenter.ScanCriteria();
        return SenseDevice.scan(deviceCenter, scanCriteria);
    }

    public @Nullable SenseDevice bestDeviceForPairing(@NonNull List<SenseDevice> devices) {
        logEvent("bestDeviceForPairing(" + devices + ")");

        if (devices.isEmpty()) {
            return null;
        } else {
            return Collections.max(devices, (l, r) -> Functions.compareInts(l.getScannedRssi(), r.getScannedRssi()));
        }
    }

    public Observable<SenseDevice> rediscoverDevice() {
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
            DeviceCenter.ScanCriteria scanCriteria = new DeviceCenter.ScanCriteria();
            scanCriteria.addAddress(deviceAddress);
            scanCriteria.setLimit(1);
            this.repairingTask = SenseDevice.scan(deviceCenter, scanCriteria).flatMap(devices -> {
                if (!devices.isEmpty()) {
                    logEvent("rediscoveredDevice(" + device + ")");
                    this.device = devices.get(0);
                    this.repairingTask = null;

                    return Observable.just(device);
                } else {
                    return Observable.error(new Exception("Could not rediscover device."));
                }
            });
            return repairingTask;
        }
    }

    public Observable<SenseDevice> connectToDevice(@NonNull SenseDevice device) {
        logEvent("connectToDevice(" + device + ")");

        if (device.isConnected() && device.getBondStatus() != is.hello.sense.bluetooth.stacks.Device.BOND_BONDED) {
            logEvent("already paired with device " + device);

            return Observable.just(null);
        }

        return device.connect().doOnNext(ignored -> {
            logEvent("pairedWithDevice(" + device + ")");
            setPairedDeviceAddress(device.getAddress());
            this.device = device;
        });
    }

    public Observable<SenseDevice> reconnect() {
        logEvent("reconnect()");

        if (device == null) {
            return noDeviceError();
        }

        return Observable.create((Observable.OnSubscribe<SenseDevice>) s -> device.disconnect().subscribe(ignored -> device.connect().subscribe(s), s::onError))
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public Observable<List<SenseBle.wifi_endpoint>> scanForWifiNetworks() {
        if (device == null) {
            return noDeviceError();
        }

        return device.scanForWifiNetworks()
                     .subscribeOn(AndroidSchedulers.mainThread())
                     .doOnError(this.respondToError)
                     .map(networks -> {
                         if (!networks.isEmpty())
                             Collections.sort(networks, (l, r) -> Functions.compareInts(l.getRssi(), r.getRssi()));

                         return networks;
                     });
    }

    public Observable<List<SenseBle.wifi_endpoint>> scanForWifiNetworks(int passCount) {
        if (passCount == 0)
            throw new IllegalArgumentException("passCount == 0");

        if (passCount == 1)
            return scanForWifiNetworks();

        return Observable.create((Observable.OnSubscribe<List<SenseBle.wifi_endpoint>>) s -> {
            Scheduler scheduler = Schedulers.computation();
            Observer<List<SenseBle.wifi_endpoint>> observer = new Observer<List<SenseBle.wifi_endpoint>>() {
                int pass = 0;
                Map<ByteString, SenseBle.wifi_endpoint> accumulator = new HashMap<>();

                @Override
                public void onCompleted() {
                    if (++pass < passCount) {
                        scanForWifiNetworks().subscribeOn(scheduler).subscribe(this);
                    } else {
                        List<SenseBle.wifi_endpoint> results = new ArrayList<>();
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
                public void onNext(List<SenseBle.wifi_endpoint> wifi_endpoints) {
                    for (SenseBle.wifi_endpoint endpoint : wifi_endpoints) {
                        accumulator.put(endpoint.getBssid(), endpoint);
                    }
                }
            };
            scanForWifiNetworks().subscribeOn(scheduler).subscribe(observer);
        });
    }

    public Observable<Void> sendWifiCredentials(String bssid, String ssid, SenseBle.wifi_endpoint.sec_type securityType, String password) {
        logEvent("sendWifiCredentials()");

        if (device == null) {
            return noDeviceError();
        }

        return device.setWifiNetwork(bssid, ssid, securityType, password)
                     .subscribeOn(AndroidSchedulers.mainThread())
                     .doOnError(this.respondToError);
    }

    public Observable<Void> linkAccount() {
        logEvent("linkAccount()");

        if (device == null) {
            return noDeviceError();
        }

        return device.linkAccount(apiSessionManager.getAccessToken())
                     .doOnError(this.respondToError);
    }

    public Observable<String> linkPill() {
        logEvent("linkPill()");

        if (device == null) {
            return noDeviceError();
        }

        return device.pairPill(apiSessionManager.getAccessToken())
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

        return device.setPairingModeEnabled(true)
                     .doOnError(this.respondToError);
    }

    public Observable<Void> factoryReset() {
        logEvent("factoryReset()");

        if (device == null) {
            return noDeviceError();
        }

        return device.factoryReset()
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
