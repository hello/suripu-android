package is.hello.sense.interactors.hardware;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import is.hello.buruberi.bluetooth.errors.BuruberiException;
import is.hello.buruberi.bluetooth.errors.ConnectionStateException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.model.SenseLedAnimation;
import is.hello.commonsense.util.Compatibility;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PendingObservables;
import is.hello.sense.interactors.Interactor;
import rx.Observable;
import rx.functions.Action1;

/**
 * Contains operations to perform basic discovery, connection, and factory reset
 * of {@link is.hello.commonsense.bluetooth.SensePeripheral}
 */
public abstract class BaseHardwareInteractor extends Interactor {

    public static final String ACTION_CONNECTION_LOST = BaseHardwareInteractor.class.getSimpleName() + ".ACTION_CONNECTION_LOST";

    private static final String TOKEN_DISCOVERY = BaseHardwareInteractor.class.getSimpleName() + ".TOKEN_DISCOVERY";
    private static final String TOKEN_CONNECT = BaseHardwareInteractor.class.getSimpleName() + ".TOKEN_CONNECT";
    private static final String TOKEN_FACTORY_RESET = BaseHardwareInteractor.class.getSimpleName() + ".TOKEN_FACTORY_RESET";

    protected final Context context;
    protected final BluetoothStack bluetoothStack;

    protected final PendingObservables<String> pending = new PendingObservables<>();
    protected final Action1<Throwable> respondToError;
    public final Observable<Boolean> bluetoothEnabled;

    @VisibleForTesting
    @Nullable
    SensePeripheral peripheral;
    protected boolean wantsHighPowerPreScan = false;

    public BaseHardwareInteractor(@NonNull final Context context,
                                  @NonNull final BluetoothStack bluetoothStack){
        this.context = context;
        this.bluetoothStack = bluetoothStack;
        this.respondToError = e -> {
            if (BuruberiException.isInstabilityLikely(e)) {
                clearPeripheral();
            } else if (e instanceof ConnectionStateException) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_CONNECTION_LOST));
            }
        };

        this.bluetoothEnabled = bluetoothStack.enabled();
        bluetoothEnabled.subscribe(this::onBluetoothEnabledChanged, Functions.LOG_ERROR);
    }

    //region bluetoothstack

    public void onBluetoothEnabledChanged(final boolean enabled) {
        logEvent("onBluetoothEnabledChanged(" + enabled + ")");
        if (!enabled) {
            this.peripheral = null;
        }
    }

    public Observable<Void> turnOnBluetooth() {
        return bluetoothStack.turnOn();
    }

    public Observable<Void> turnOffBluetooth() {
        return bluetoothStack.turnOff();
    }

    //endregion

    public void setPeripheral(@Nullable final SensePeripheral peripheral) {
        this.peripheral = peripheral;
    }

    public void setWantsHighPowerPreScan(final boolean wantsHighPowerPreScan) {
        logEvent("setWantsHighPowerPreScan(" + wantsHighPowerPreScan + ")");
        this.wantsHighPowerPreScan = wantsHighPowerPreScan;
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

    public
    @Nullable
    String getDeviceId() {
        if (peripheral != null) {
            return peripheral.getDeviceId();
        } else {
            return null;
        }
    }

    public boolean isDeviceSupported() {
        return Compatibility.generateReport(context).isSupported();
    }

    public Observable<SensePeripheral> discoverPeripheralForDevice(@NonNull final SenseDevice device) {
        logEvent("discoverPeripheralForDevice(" + device.deviceId + ")");

        if (TextUtils.isEmpty(device.deviceId))
            throw new IllegalArgumentException("Malformed Sense device " + device);

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
            }).doOnError(e -> {
                logEvent("failed to pair with peripheral " + peripheral + ": " + e);
            });
        });
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

    public Observable<Void> runLedAnimation(@NonNull final SenseLedAnimation animationType) {
        logEvent("runLedAnimation()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return peripheral.runLedAnimation(animationType)
                         .doOnError(this.respondToError);
    }

    public Observable<Void> unsafeFactoryReset() {
        logEvent("unsafeFactoryReset()");

        if (peripheral == null) {
            return noDeviceError();
        }

        return pending.bind(TOKEN_FACTORY_RESET, () -> peripheral.factoryReset().doOnError(this.respondToError));
    }

    /**
     * Call this method to clear values.
     * Usually done at end of successful flow
     */
    @CallSuper
    public void reset(){
        wantsHighPowerPreScan = false;
        clearPeripheral();
    }

    protected
    @NonNull
    <T> Observable<T> noDeviceError() {
        return Observable.error(new NoConnectedPeripheralException());
    }

    public static class NoConnectedPeripheralException extends BuruberiException {
        public NoConnectedPeripheralException() {
            super("HardwareInteractor peripheral method called without paired peripheral.",
                  new NullPointerException());
        }
    }

}
