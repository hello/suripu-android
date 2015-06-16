package is.hello.sense.bluetooth.devices;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import java.util.UUID;

import is.hello.sense.R;
import is.hello.sense.bluetooth.errors.PeripheralConnectionError;
import is.hello.sense.bluetooth.stacks.OperationTimeout;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.util.Logger;
import rx.Observable;

/**
 * Semi-high level wrapper around a Peripheral. Provides generic connection and subscription functionality.
 */
public abstract class HelloPeripheral<TSelf extends HelloPeripheral<TSelf>> {
    protected final Peripheral peripheral;
    protected PeripheralService peripheralService;

    protected HelloPeripheral(@NonNull Peripheral peripheral) {
        this.peripheral = peripheral;
    }

    //region Properties

    public int getScannedRssi() {
        return peripheral.getScanTimeRssi();
    }

    public String getAddress() {
        return peripheral.getAddress();
    }

    public String getName() {
        return peripheral.getName();
    }

    public AdvertisingData getAdvertisingData() {
        return peripheral.getAdvertisingData();
    }

    //endregion


    //region Connectivity

    /**
     * Connects to the peripheral, ensures a bond is present, and performs service discovery.
     * This method should be wrapped by subclasses so that the caller does not have to provide
     * an operation timeout object.
     * @param timeout   A timeout object to apply to the service discovery portion of connection.
     */
    protected Observable<ConnectStatus> connect(@NonNull OperationTimeout timeout) {
        Observable<ConnectStatus> sequence;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Some Lollipop devices (not all!) do not support establishing
            // bonds after connecting. This is the exact opposite of the
            // behavior in KitKat and Gingerbread, which cannot establish
            // bonds without an active connection.
            sequence = Observable.concat(
                Observable.just(ConnectStatus.BONDING),
                peripheral.createBond().map(ignored -> ConnectStatus.CONNECTING),
                peripheral.connect(timeout).map(ignored -> ConnectStatus.DISCOVERING_SERVICES),
                peripheral.discoverService(getTargetServiceIdentifier(), timeout).map(service -> {
                    this.peripheralService = service;
                    return ConnectStatus.CONNECTED;
                })
            );
        } else {
            sequence = Observable.concat(
                Observable.just(ConnectStatus.CONNECTING),
                peripheral.connect(timeout).map(ignored -> ConnectStatus.BONDING),
                peripheral.createBond().map(ignored -> ConnectStatus.DISCOVERING_SERVICES),
                peripheral.discoverService(getTargetServiceIdentifier(), timeout).map(service -> {
                    this.peripheralService = service;
                    return ConnectStatus.CONNECTED;
                })
            );
        }

        return sequence.subscribeOn(peripheral.getStack().getScheduler())
                       .doOnNext(s -> Logger.info(getClass().getSimpleName(), "is " + s))
                       .doOnError(connectError -> {
                           if (isConnected()) {
                               Logger.warn(getClass().getSimpleName(), "Disconnecting after failed connection attempt.", connectError);
                               disconnect().subscribe(ignored -> {
                                   Logger.info(getClass().getSimpleName(), "Disconnected after failed connection attempt.");
                               }, disconnectError -> {
                                   Logger.error(getClass().getSimpleName(), "Disconnected after failed connection attempt failed, ignoring.", disconnectError);
                               });
                           }
                       });
    }

    public Observable<TSelf> disconnect() {
        //noinspection unchecked
        return peripheral.disconnect()
                .map(ignored -> (TSelf) this)
                .finallyDo(() -> {
                    this.peripheralService = null;
                });
    }

    public boolean isConnected() {
        return (peripheral.getConnectionStatus() == Peripheral.STATUS_CONNECTED &&
                peripheralService != null);
    }

    public int getBondStatus() {
        return peripheral.getBondStatus();
    }

    //endregion


    //region Internal

    protected abstract UUID getTargetServiceIdentifier();
    protected abstract UUID getDescriptorIdentifier();

    protected PeripheralService getTargetService() {
        return peripheralService;
    }

    protected Observable<UUID> subscribe(@NonNull UUID characteristicIdentifier,
                                         @NonNull OperationTimeout timeout) {
        Logger.info(Peripheral.LOG_TAG, "Subscribing to " + characteristicIdentifier);

        if (!isConnected()) {
            return Observable.error(new PeripheralConnectionError());
        }

        return peripheral.subscribeNotification(getTargetService(),
                characteristicIdentifier,
                getDescriptorIdentifier(),
                timeout);
    }

    protected Observable<UUID> unsubscribe(@NonNull UUID characteristicIdentifier,
                                           @NonNull OperationTimeout timeout) {
        Logger.info(Peripheral.LOG_TAG, "Unsubscribing from " + characteristicIdentifier);

        if (isConnected()) {
            return peripheral.unsubscribeNotification(getTargetService(),
                    characteristicIdentifier,
                    getDescriptorIdentifier(),
                    timeout);
        } else {
            return Observable.just(characteristicIdentifier);
        }
    }

    //endregion


    @Override
    public String toString() {
        return '{' + getClass().getSimpleName() + ' ' + getName() + '@' + getAddress() + '}';
    }


    public enum ConnectStatus {
        CONNECTING(R.string.title_connecting),
        BONDING(R.string.title_pairing),
        DISCOVERING_SERVICES(R.string.title_discovering_services),
        CONNECTED(0);

        public final @StringRes int messageRes;

        ConnectStatus(@StringRes int messageRes) {
            this.messageRes = messageRes;
        }
    }


    public static class Tests {
        public static void setPeripheralService(@NonNull HelloPeripheral<? extends HelloPeripheral> peripheral, @Nullable PeripheralService service) {
            peripheral.peripheralService = service;
        }
    }
}
