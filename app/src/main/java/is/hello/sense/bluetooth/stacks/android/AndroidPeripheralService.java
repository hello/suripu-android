package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.PeripheralService;

public final class AndroidPeripheralService implements PeripheralService {
    final @NonNull BluetoothGattService service;

    static @NonNull Map<UUID, PeripheralService> wrapNativeServices(@NonNull List<BluetoothGattService> nativeServices) {
        Map<UUID, PeripheralService> peripheralServices = new HashMap<>();

        for (BluetoothGattService nativeService : nativeServices) {
            peripheralServices.put(nativeService.getUuid(), new AndroidPeripheralService(nativeService));
        }

        return peripheralServices;
    }

    AndroidPeripheralService(@NonNull BluetoothGattService service) {
        this.service = service;
    }


    @Override
    public UUID getUuid() {
        return service.getUuid();
    }

    @Override
    public int getType() {
        return service.getType();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AndroidPeripheralService that = (AndroidPeripheralService) o;

        return service.equals(that.service);

    }

    @Override
    public int hashCode() {
        return service.hashCode();
    }


    @Override
    public String toString() {
        return "NativeService{" +
                "service=" + service +
                '}';
    }
}
