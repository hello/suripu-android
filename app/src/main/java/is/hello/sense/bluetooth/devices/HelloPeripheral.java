package is.hello.sense.bluetooth.devices;

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
        return peripheral.getStack().newConfiguredObservable(s -> {
            Logger.info(Peripheral.LOG_TAG, "connect to " + toString());

            s.onNext(ConnectStatus.CONNECTING);
            peripheral.connect(timeout).subscribe(peripheral -> {
                Logger.info(Peripheral.LOG_TAG, "connected to " + toString());

                s.onNext(ConnectStatus.DISCOVERING_SERVICES);
                peripheral.discoverService(getTargetServiceIdentifier(), timeout).subscribe(service -> {
                    Logger.info(Peripheral.LOG_TAG, "discovered service for " + toString());

                    this.peripheralService = service;
                    s.onNext(ConnectStatus.CONNECTED);
                    s.onCompleted();
                }, e -> {
                    // discoverServices took ownership of timeout,
                    // we don't need to worry about it anymore.

                    Logger.error(Peripheral.LOG_TAG, "Disconnecting due to service discovery failure", e);
                    disconnect().subscribe(ignored -> {
                        Logger.info(Peripheral.LOG_TAG, "Disconnected from service discovery failure");
                        s.onError(e);
                    }, disconnectError -> {
                        Logger.error(Peripheral.LOG_TAG, "Could not disconnect for service discovery failure", disconnectError);
                        s.onError(e);
                    });
                });
            }, e -> {
                timeout.unschedule();

                s.onError(e);
            });
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


    public static enum ConnectStatus {
        CONNECTING(R.string.title_connecting),
        DISCOVERING_SERVICES(R.string.title_discovering_services),
        CONNECTED(0);

        public final @StringRes int messageRes;

        private ConnectStatus(@StringRes int messageRes) {
            this.messageRes = messageRes;
        }
    }


    public static class Tests {
        public static void setPeripheralService(@NonNull HelloPeripheral<? extends HelloPeripheral> peripheral, @Nullable PeripheralService service) {
            peripheral.peripheralService = service;
        }
    }
}
