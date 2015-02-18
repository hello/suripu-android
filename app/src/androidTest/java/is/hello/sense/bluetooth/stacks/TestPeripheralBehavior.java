package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.functional.Either;

public class TestPeripheralBehavior {
    final List<Method> calledMethods = new ArrayList<>();

    final @NonNull String name;
    final @NonNull String address;
    final int scanTimeRssi;

    AdvertisingData advertisingData = AdvertisingData.parse(new byte[0]);

    @Peripheral.ConnectivityStatus int connectionStatus = Peripheral.STATUS_DISCONNECTED;
    @Peripheral.BondStatus int bondStatus = Peripheral.BOND_NONE;
    Either<Collection<PeripheralService>, Throwable> servicesResponse;
    Either<Peripheral, Throwable> connectResponse;
    Either<Peripheral, Throwable> disconnectResponse;
    Either<Peripheral, Throwable> createBondResponse;
    Either<Peripheral, Throwable> removeBondResponse;
    Either<UUID, Throwable> subscriptionResponse;
    Either<UUID, Throwable> unsubscriptionResponse;
    Either<Void, Throwable> writeCommandResponse;
    long latency = 0L;

    public TestPeripheralBehavior(@NonNull String name, @NonNull String address, int scanTimeRssi) {
        this.name = name;
        this.address = address;
        this.scanTimeRssi = scanTimeRssi;
    }

    public TestPeripheralBehavior setAdvertisingData(@NonNull AdvertisingData advertisingData) {
        this.advertisingData = advertisingData;
        return this;
    }

    public TestPeripheralBehavior setServicesResponse(Either<Collection<PeripheralService>, Throwable> servicesResponse) {
        this.servicesResponse = servicesResponse;
        return this;
    }

    public TestPeripheralBehavior setConnectionStatus(@Peripheral.ConnectivityStatus int connectionStatus) {
        this.connectionStatus = connectionStatus;
        return this;
    }

    public TestPeripheralBehavior setBondStatus(int bondStatus) {
        this.bondStatus = bondStatus;
        return this;
    }

    public TestPeripheralBehavior setConnectResponse(@NonNull Either<Peripheral, Throwable> connectResponse) {
        this.connectResponse = connectResponse;
        return this;
    }

    public TestPeripheralBehavior setDisconnectResponse(@NonNull Either<Peripheral, Throwable> disconnectResponse) {
        this.disconnectResponse = disconnectResponse;
        return this;
    }

    public TestPeripheralBehavior setCreateBondResponse(@NonNull Either<Peripheral, Throwable> createBondResponse) {
        this.createBondResponse = createBondResponse;
        return this;
    }

    public TestPeripheralBehavior setRemoveBondResponse(@NonNull Either<Peripheral, Throwable> removeBondResponse) {
        this.removeBondResponse = removeBondResponse;
        return this;
    }

    public TestPeripheralBehavior setSubscriptionResponse(@NonNull Either<UUID, Throwable> subscriptionResponse) {
        this.subscriptionResponse = subscriptionResponse;
        return this;
    }

    public TestPeripheralBehavior setUnsubscriptionResponse(@NonNull Either<UUID, Throwable> unsubscriptionResponse) {
        this.unsubscriptionResponse = unsubscriptionResponse;
        return this;
    }

    public TestPeripheralBehavior setWriteCommandResponse(Either<Void, Throwable> writeCommandResponse) {
        this.writeCommandResponse = writeCommandResponse;
        return this;
    }

    public TestPeripheralBehavior setLatency(long latency) {
        this.latency = latency;
        return this;
    }

    public TestPeripheralBehavior reset() {
        calledMethods.clear();
        this.connectionStatus = Peripheral.STATUS_DISCONNECTED;
        this.bondStatus = Peripheral.BOND_NONE;
        this.servicesResponse = null;
        this.connectResponse = null;
        this.disconnectResponse = null;
        this.createBondResponse = null;
        this.removeBondResponse = null;
        this.subscriptionResponse = null;
        this.unsubscriptionResponse = null;
        this.writeCommandResponse = null;
        this.latency = 0L;

        return this;
    }


    void trackMethodCall(@NonNull Method method) {
        calledMethods.add(method);
    }

    public List<Method> getCalledMethods() {
        return calledMethods;
    }

    public boolean wasMethodCalled(@NonNull Method method) {
        return calledMethods.contains(method);
    }


    public static enum Method {
        CONNECT,
        DISCONNECT,
        CREATE_BOND,
        REMOVE_BOND,
        DISCOVER_SERVICES,
        SUBSCRIBE,
        UNSUBSCRIBE,
        WRITE_COMMAND,
    }
}
