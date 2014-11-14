package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.bluetooth.stacks.util.TakesOwnership;
import rx.Observable;

public class TestPeripheral implements Peripheral {
    final BluetoothStack stack;
    final TestPeripheralConfig peripheralConfig;

    PacketHandler dataHandler;

    public TestPeripheral(@NonNull BluetoothStack stack,
                          @NonNull TestPeripheralConfig peripheralConfig) {
        this.stack = stack;
        this.peripheralConfig = peripheralConfig;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private <T> Observable<T> createResponseWith(T value, @Nullable OperationTimeout timeout) {
        Observable<T> observable;
        if (peripheralConfig.currentError == null) {
            observable = Observable.just(value);
        } else {
            observable = Observable.error(peripheralConfig.currentError);
        }
        return observable.delay(peripheralConfig.latency, TimeUnit.MILLISECONDS).finallyDo(timeout::recycle);
    }

    @Override
    public int getScanTimeRssi() {
        return peripheralConfig.scanTimeRssi;
    }

    @Override
    public String getAddress() {
        return peripheralConfig.address;
    }

    @Override
    public String getName() {
        return peripheralConfig.name;
    }

    @Override
    public BluetoothStack getStack() {
        return stack;
    }

    @NonNull
    @Override
    public Observable<Peripheral> connect() {
        return createResponseWith(this, null);
    }

    @NonNull
    @Override
    public Observable<Peripheral> disconnect() {
        return createResponseWith(this, null);
    }

    @Override
    public int getConnectionStatus() {
        return peripheralConfig.connectionStatus;
    }

    @NonNull
    @Override
    public Observable<Peripheral> createBond() {
        return createResponseWith(this, null);
    }

    @NonNull
    @Override
    public Observable<Peripheral> removeBond() {
        return createResponseWith(this, null);
    }

    @Override
    public int getBondStatus() {
        return peripheralConfig.bondStatus;
    }

    @NonNull
    @Override
    public Observable<Collection<PeripheralService>> discoverServices(@NonNull @TakesOwnership OperationTimeout timeout) {
        return createResponseWith(peripheralConfig.services.values(), timeout);
    }

    @Nullable
    @Override
    public PeripheralService getService(@NonNull UUID serviceIdentifier) {
        return peripheralConfig.services.get(serviceIdentifier);
    }

    @NonNull
    @Override
    public Observable<UUID> subscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                  @NonNull UUID characteristicIdentifier,
                                                  @NonNull UUID descriptorIdentifier,
                                                  @NonNull @TakesOwnership OperationTimeout timeout) {
        return createResponseWith(peripheralConfig.subscriptionResponse, timeout);
    }

    @NonNull
    @Override
    public Observable<UUID> unsubscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier,
                                                    @NonNull @TakesOwnership OperationTimeout timeout) {
        return createResponseWith(peripheralConfig.subscriptionResponse, timeout);
    }

    @NonNull
    @Override
    public Observable<Void> writeCommand(@NonNull PeripheralService onPeripheralService,
                                         @NonNull UUID identifier,
                                         @NonNull byte[] payload,
                                         @NonNull @TakesOwnership OperationTimeout timeout) {
        return createResponseWith(null, timeout);
    }

    @Override
    public void setPacketHandler(@Nullable PacketHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @Nullable
    @Override
    public PacketHandler getPacketHandler() {
        return this.dataHandler;
    }
}
