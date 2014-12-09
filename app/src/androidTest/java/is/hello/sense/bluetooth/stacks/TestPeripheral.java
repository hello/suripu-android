package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.bluetooth.stacks.util.TakesOwnership;
import is.hello.sense.functional.Either;
import is.hello.sense.functional.Lists;
import rx.Observable;

public class TestPeripheral implements Peripheral {
    final BluetoothStack stack;
    final TestPeripheralBehavior behavior;

    PacketHandler dataHandler;

    public TestPeripheral(@NonNull BluetoothStack stack,
                          @NonNull TestPeripheralBehavior peripheralBehavior) {
        this.stack = stack;
        this.behavior = peripheralBehavior;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private <T> Observable<T> createResponseWith(@NonNull Either<T, Throwable> value, @Nullable OperationTimeout timeout) {
        // This isn't really the intended order here, but
        // `finallyDo` appears to be non-deterministic.
        if (timeout != null) {
            timeout.recycle();
        }

        Observable<T> observable = value.<Observable<T>>map(Observable::just, Observable::error);
        return observable.delay(behavior.latency, TimeUnit.MILLISECONDS);
    }

    @Override
    public int getScanTimeRssi() {
        return behavior.scanTimeRssi;
    }

    @Override
    public String getAddress() {
        return behavior.address;
    }

    @Override
    public String getName() {
        return behavior.name;
    }

    @Override
    public BluetoothStack getStack() {
        return stack;
    }

    @NonNull
    @Override
    public Observable<Peripheral> connect() {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.CONNECT);
        return createResponseWith(behavior.connectResponse, null);
    }

    @NonNull
    @Override
    public Observable<Peripheral> disconnect() {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.DISCONNECT);
        return createResponseWith(behavior.disconnectResponse, null);
    }

    @Override
    public int getConnectionStatus() {
        return behavior.connectionStatus;
    }

    @NonNull
    @Override
    public Observable<Peripheral> createBond() {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.CREATE_BOND);
        return createResponseWith(behavior.createBondResponse, null);
    }

    @NonNull
    @Override
    public Observable<Peripheral> removeBond() {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.REMOVE_BOND);
        return createResponseWith(behavior.removeBondResponse, null);
    }

    @Override
    public int getBondStatus() {
        return behavior.bondStatus;
    }

    @NonNull
    @Override
    public Observable<Collection<PeripheralService>> discoverServices(@NonNull @TakesOwnership OperationTimeout timeout) {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.DISCOVER_SERVICES);
        return createResponseWith(behavior.servicesResponse, timeout);
    }

    @Nullable
    @Override
    public PeripheralService getService(@NonNull UUID serviceIdentifier) {
        if (behavior.servicesResponse != null && behavior.servicesResponse.isLeft()) {
            return Lists.findFirst(behavior.servicesResponse.getLeft(), s -> s.getUuid().equals(serviceIdentifier));
        } else {
            return null;
        }
    }

    @NonNull
    @Override
    public Observable<UUID> subscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                  @NonNull UUID characteristicIdentifier,
                                                  @NonNull UUID descriptorIdentifier,
                                                  @NonNull @TakesOwnership OperationTimeout timeout) {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.SUBSCRIBE);
        return createResponseWith(behavior.subscriptionResponse, timeout);
    }

    @NonNull
    @Override
    public Observable<UUID> unsubscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier,
                                                    @NonNull @TakesOwnership OperationTimeout timeout) {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.UNSUBSCRIBE);
        return createResponseWith(behavior.unsubscriptionResponse, timeout);
    }

    @NonNull
    @Override
    public Observable<Void> writeCommand(@NonNull PeripheralService onPeripheralService,
                                         @NonNull UUID identifier,
                                         @NonNull byte[] payload,
                                         @NonNull @TakesOwnership OperationTimeout timeout) {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.WRITE_COMMAND);
        return createResponseWith(behavior.writeCommandResponse, timeout);
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
