package is.hello.sense.bluetooth.devices;

import android.support.annotation.NonNull;

import java.util.UUID;

import is.hello.sense.bluetooth.errors.TooManyOperationsError;
import is.hello.sense.bluetooth.stacks.OperationTimeout;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.util.Logger;
import rx.Observable;

public abstract class HelloPeripheral<TSelf extends HelloPeripheral<TSelf>> {
    protected final Peripheral peripheral;
    protected final OperationTimeout commonTimeout;
    protected PeripheralService peripheralService;

    protected HelloPeripheral(@NonNull Peripheral peripheral,
                              @NonNull OperationTimeout timeout) {
        this.peripheral = peripheral;
        this.commonTimeout = timeout;
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

    //endregion


    //region Connectivity

    public Observable<ConnectStatus> connect() {
        return peripheral.getStack().newConfiguredObservable(s -> {
            Logger.info(Peripheral.LOG_TAG, "connect to " + toString());

            s.onNext(ConnectStatus.CONNECTING);
            peripheral.connect().subscribe(device -> {
                Logger.info(Peripheral.LOG_TAG, "connected to " + toString());

                s.onNext(ConnectStatus.BONDING);
                device.createBond().subscribe(ignored -> {
                    Logger.info(Peripheral.LOG_TAG, "bonded to " + toString());

                    s.onNext(ConnectStatus.DISCOVERING_SERVICES);
                    device.discoverServices(commonTimeout).subscribe(services -> {
                        Logger.info(Peripheral.LOG_TAG, "discovered services for " + toString());

                        this.peripheralService = device.getService(getTargetServiceIdentifier());
                        s.onNext(ConnectStatus.CONNECTED);
                        s.onCompleted();
                    }, s::onError);
                }, s::onError);
            }, s::onError);
        });
    }

    public Observable<TSelf> disconnect() {
        //noinspection unchecked
        return peripheral.disconnect().map(ignored -> (TSelf) this);
    }

    public boolean isConnected() {
        return peripheral.getConnectionStatus() == Peripheral.STATUS_CONNECTED;
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

    protected Observable<UUID> subscribe(@NonNull UUID characteristicIdentifier) {
        Logger.info(Peripheral.LOG_TAG, "Subscribing to " + characteristicIdentifier);

        if (commonTimeout.isScheduled())
            return Observable.error(new TooManyOperationsError());
        else
            return peripheral.subscribeNotification(getTargetService(), characteristicIdentifier, getDescriptorIdentifier(), commonTimeout);
    }

    protected Observable<UUID> unsubscribe(@NonNull UUID characteristicIdentifier) {
        Logger.info(Peripheral.LOG_TAG, "Unsubscribing from " + characteristicIdentifier);

        if (commonTimeout.isScheduled())
            return Observable.error(new TooManyOperationsError());
        else
            return peripheral.unsubscribeNotification(getTargetService(), characteristicIdentifier, getDescriptorIdentifier(), commonTimeout);
    }

    //endregion


    @Override
    public String toString() {
        return '{' + getClass().getSimpleName() + ' ' + getName() + '@' + getAddress() + '}';
    }


    public static enum ConnectStatus {
        CONNECTING,
        BONDING,
        DISCOVERING_SERVICES,
        CONNECTED,
    }
}
