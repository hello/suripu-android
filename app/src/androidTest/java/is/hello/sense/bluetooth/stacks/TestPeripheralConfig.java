package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class TestPeripheralConfig {
    final @NonNull String name;
    final @NonNull String address;
    final int scanTimeRssi;

    @NonNull Map<UUID, PeripheralService> services = Collections.emptyMap();
    @Nullable Throwable currentError;
    int connectionStatus = Peripheral.STATUS_DISCONNECTED;
    int bondStatus = Peripheral.BOND_NONE;
    UUID subscriptionResponse;
    UUID unsubscriptionResponse;
    long latency = 0L;

    public TestPeripheralConfig(@NonNull String name, @NonNull String address, int scanTimeRssi) {
        this.name = name;
        this.address = address;
        this.scanTimeRssi = scanTimeRssi;
    }

    public TestPeripheralConfig withServices(@NonNull Map<UUID, PeripheralService> services) {
        this.services = services;
        return this;
    }

    public TestPeripheralConfig withConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
        return this;
    }

    public TestPeripheralConfig withBondStatus(int bondStatus) {
        this.bondStatus = bondStatus;
        return this;
    }

    public TestPeripheralConfig withSubscriptionResponse(@NonNull UUID subscriptionResponse) {
        this.subscriptionResponse = subscriptionResponse;
        return this;
    }

    public TestPeripheralConfig withUnsubscriptionResponse(@NonNull UUID unsubscriptionResponse) {
        this.unsubscriptionResponse = unsubscriptionResponse;
        return this;
    }

    public TestPeripheralConfig withLatency(long latency) {
        this.latency = latency;
        return this;
    }

    public TestPeripheralConfig withCurrentError(@Nullable Throwable currentError) {
        this.currentError = currentError;
        return this;
    }

    public TestPeripheralConfig reset() {
        this.services = Collections.emptyMap();
        this.currentError = null;
        this.connectionStatus = Peripheral.STATUS_DISCONNECTED;
        this.bondStatus = Peripheral.BOND_NONE;
        this.subscriptionResponse = null;
        this.unsubscriptionResponse = null;
        this.latency = 0L;

        return this;
    }
}
