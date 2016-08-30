package is.hello.sense.interactors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.errors.BuruberiException;
import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.Rx;
import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.bluetooth.model.SenseConnectToWiFiUpdate;
import is.hello.commonsense.bluetooth.model.SenseLedAnimation;
import is.hello.commonsense.bluetooth.model.SenseNetworkStatus;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.Compatibility;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PendingObservables;
import rx.Observable;
import rx.functions.Action1;

@Singleton public class HardwareInteractor extends Interactor {
    public static final String ACTION_CONNECTION_LOST = HardwareInteractor.class.getName() + ".ACTION_CONNECTION_LOST";
    public static final int FAILS_BEFORE_HIGH_POWER = 2;

    private static final String TOKEN_DISCOVERY = HardwareInteractor.class.getSimpleName() + ".TOKEN_DISCOVERY";
    private static final String TOKEN_CONNECT = HardwareInteractor.class.getSimpleName() + ".TOKEN_CONNECT";
    private static final String TOKEN_GET_WIFI = HardwareInteractor.class.getSimpleName() + ".TOKEN_GET_WIFI";
    private static final String TOKEN_FACTORY_RESET = HardwareInteractor.class.getSimpleName() + ".TOKEN_FACTORY_RESET";

    private final Context context;
    private final PreferencesInteractor preferencesPresenter;
    private final ApiSessionManager apiSessionManager;
    private final DevicesInteractor devicesPresenter;
    private final BluetoothStack bluetoothStack;

    private final PendingObservables<String> pending = new PendingObservables<>();

    @VisibleForTesting @Nullable SensePeripheral peripheral;

    private int peripheralNotFoundCount = 0;
    private boolean wantsHighPowerPreScan;

    private final Action1<Throwable> respondToError;

    public final Observable<Boolean> bluetoothEnabled;

    @Inject public HardwareInteractor(@NonNull final Context context,
                                      @NonNull final PreferencesInteractor preferencesInteractor,
                                      @NonNull final ApiSessionManager apiSessionManager,
                                      @NonNull final DevicesInteractor devicesInteractor,
                                      @NonNull final BluetoothStack bluetoothStack) {
        this.context = context;
        this.preferencesPresenter = preferencesInteractor;
        this.apiSessionManager = apiSessionManager;
        this.devicesPresenter = devicesInteractor;
        this.bluetoothStack = bluetoothStack;

        this.respondToError = e -> {
            if (BuruberiException.isInstabilityLikely(e)) {
                clearPeripheral();
            } else if (e != null && e instanceof ConnectionStateException) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_CONNECTION_LOST));
            }
        };

        final Observable<Intent> logOutSignal = Rx.fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        logOutSignal.subscribe(this::onUserLoggedOut, Functions.LOG_ERROR);

        final Observable<Intent> disconnectSignal = Rx.fromLocalBroadcast(context, new IntentFilter(GattPeripheral.ACTION_DISCONNECTED));
        disconnectSignal.subscribe(this::onPeripheralDisconnected, Functions.LOG_ERROR);

        this.bluetoothEnabled = bluetoothStack.enabled();
        bluetoothEnabled.subscribe(this::onBluetoothEnabledChanged, Functions.LOG_ERROR);
    }

    public void onUserLoggedOut(@NonNull final Intent ignored) {
        setLastPeripheralAddress(null);
        setPairedPillId(null);
    }

    public void onBluetoothEnabledChanged(final boolean enabled) {
        logEvent("onBluetoothEnabledChanged(" + enabled + ")");
        if (!enabled) {
            this.peripheral = null;
        }
    }

    public void onPeripheralDisconnected(@NonNull final Intent intent) {
        if (peripheral != null) {
            final String currentAddress = peripheral.getAddress();
            final String intentAddress = intent.getStringExtra(GattPeripheral.EXTRA_ADDRESS);
            if (TextUtils.equals(currentAddress, intentAddress)) {
                logEvent("broadcasting disconnect");

                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_CONNECTION_LOST));
            }
        }
    }


    public void setLastPeripheralAddress(@Nullable final String address) {
        logEvent("saving paired peripheral address: " + address);

        SharedPreferences.Editor editor = preferencesPresenter.edit();
        if (address != null) {
            editor.putString(PreferencesInteractor.PAIRED_DEVICE_ADDRESS, address);
        } else {
            editor.remove(PreferencesInteractor.PAIRED_DEVICE_ADDRESS);
        }
        editor.apply();
    }

    public void setPairedPillId(@Nullable final String pillId) {
        logEvent("saving paired pill id: " + pillId);

        final SharedPreferences.Editor editor = preferencesPresenter.edit();
        if (pillId != null) {
            editor.putString(PreferencesInteractor.PAIRED_PILL_ID, pillId);
        } else {
            editor.remove(PreferencesInteractor.PAIRED_PILL_ID);
        }
        editor.apply();
    }


    public void trackPeripheralNotFound() {
        this.peripheralNotFoundCount++;
    }

    public boolean shouldPromptForHighPowerScan() {
        return (!wantsHighPowerPreScan && peripheralNotFoundCount >= FAILS_BEFORE_HIGH_POWER);
    }

    public void setWantsHighPowerPreScan(final boolean wantsHighPowerPreScan) {
        logEvent("setWantsHighPowerPreScan(" + wantsHighPowerPreScan + ")");
        this.wantsHighPowerPreScan = wantsHighPowerPreScan;
    }

    public void setPeripheral(@Nullable final SensePeripheral peripheral) {
        this.peripheral = peripheral;
    }

    public boolean hasPeripheral() {
        return (peripheral != null);
    }

    public boolean isConnected() {
        return (peripheral != null && peripheral.isConnected());
    }

    public int getBondStatus() {
        if (peripheral != null) {
            return peripheral.getBondStatus();
        } else {
            return GattPeripheral.BOND_NONE;
        }
    }

    public @Nullable String getDeviceId() {
        if (peripheral != null) {
            return peripheral.getDeviceId();
        } else {
            return null;
        }
    }

    public boolean isDeviceSupported() {
        return Compatibility.generateReport(context).isSupported();
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
                            return Observable.error(new SenseNotFoundError());
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

        String address = preferencesPresenter.getString(PreferencesInteractor.PAIRED_DEVICE_ADDRESS, null);
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
                                return Observable.error(new SenseNotFoundError());
                            }
                        });
            });
        }
    }

    public Observable<SensePeripheral> discoverPeripheralForDevice(@NonNull SenseDevice device) {
        logEvent("discoverPeripheralForDevice(" + device.deviceId + ")");

        if (TextUtils.isEmpty(device.deviceId))
            throw new IllegalArgumentException("Malformed Sense device " + device);

        if (this.peripheral != null) {
            logEvent("peripheral already discovered " + peripheral);

            return Observable.just(this.peripheral);
        }

        return pending.bind(TOKEN_DISCOVERY, () -> {
            return SensePeripheral.rediscover(bluetoothStack, device.deviceId, wantsHighPowerPreScan)
                    .flatMap(peripheral -> {
                        logEvent("rediscoveredPeripheralForDevice(" + peripheral + ")");
                        this.peripheral = peripheral;
                        return Observable.just(this.peripheral);
                    });
        });
    }

    public Observable<ConnectProgress> connectToPeripheral() {
        logEvent("connectToPeripheral(" + peripheral + ")");

        if (peripheral == null) {
            return noDeviceError();
        }

        if (peripheral.isConnected()) {
            logEvent("already paired with peripheral " + peripheral);

            return Observable.just(ConnectProgress.CONNECTED);
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

    public Observable<Void> clearBond() {
        logEvent("clearBond()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.removeBond()
                .doOnError(this.respondToError)
                .map(ignored -> null);
    }

    public Observable<Void> runLedAnimation(@NonNull final SenseLedAnimation animationType) {
        logEvent("runLedAnimation()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.runLedAnimation(animationType)
                         .doOnError(this.respondToError);
    }

    @VisibleForTesting
    void sortWifiNetworks(@NonNull final List<SenseCommandProtos.wifi_endpoint> networks) {
        if (!networks.isEmpty()) {
            Collections.sort(networks, (l, r) -> Functions.compareInts(r.getRssi(), l.getRssi()));
        }
    }

    public Observable<List<SenseCommandProtos.wifi_endpoint>> scanForWifiNetworks(final boolean sendCountryCode) {
        if (peripheral == null) {
            return noDeviceError();
        }
        SensePeripheral.CountryCode countryCode = null;
        if (sendCountryCode){
            final DateTimeZone timeZone = DateTimeZone.getDefault();
            final String timeZoneId = timeZone.getID();
            if (timeZoneId.contains("America")) {
                countryCode = SensePeripheral.CountryCode.US;
            } else if (timeZoneId.contains("Japan")) {
                countryCode = SensePeripheral.CountryCode.JP;
            } else {
                countryCode = SensePeripheral.CountryCode.EU;
            }
        }
        return peripheral.scanForWifiNetworks(countryCode)
                         .subscribeOn(Rx.mainThreadScheduler())
                         .doOnError(this.respondToError)
                         .map(networks -> {
                             sortWifiNetworks(networks);

                             return networks;
                         });
    }

    public Observable<SenseNetworkStatus> currentWifiNetwork() {
        logEvent("currentWifiNetwork()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return pending.bind(TOKEN_GET_WIFI, () -> peripheral.getWifiNetwork()
                .doOnError(this.respondToError));
    }

    public Observable<SenseConnectToWiFiUpdate> sendWifiCredentials(@NonNull final String ssid,
                                                                    @NonNull final SenseCommandProtos.wifi_endpoint.sec_type securityType,
                                                                    @NonNull final String password
                                                                   ) {
        logEvent("sendWifiCredentials()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.connectToWiFiNetwork(ssid, securityType, password)
                         .subscribeOn(Rx.mainThreadScheduler())
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

        return peripheral.putIntoPairingMode()
                         .doOnError(this.respondToError)
                         .doOnCompleted(this::clearPeripheral);
    }

    public Observable<Void> factoryReset(@NonNull SenseDevice apiDevice) {
        logEvent("factoryReset()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return pending.bind(TOKEN_FACTORY_RESET, () -> {
            Observable<VoidResponse> resetBackEnd = devicesPresenter.removeSenseAssociations(apiDevice);
            Observable<Void> resetSense = peripheral.factoryReset();
            return resetBackEnd.flatMap(ignored -> resetSense)
                    .doOnError(this.respondToError);
        });
    }

    public Observable<Void> unsafeFactoryReset() {
        logEvent("unsafeFactoryReset()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return pending.bind(TOKEN_FACTORY_RESET, () -> peripheral.factoryReset().doOnError(this.respondToError));
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

    public static class NoConnectedPeripheralException extends BuruberiException {
        public NoConnectedPeripheralException() {
            super("HardwareInteractor peripheral method called without paired peripheral.",
                  new NullPointerException());
        }
    }
}