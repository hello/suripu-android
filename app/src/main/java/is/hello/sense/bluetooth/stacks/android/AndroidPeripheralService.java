package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.stacks.PeripheralService;

public final class AndroidPeripheralService implements PeripheralService {
    public static final int SERVICE_TYPE_PRIMARY = BluetoothGattService.SERVICE_TYPE_PRIMARY;
    public static final int SERVICE_TYPE_SECONDARY = BluetoothGattService.SERVICE_TYPE_SECONDARY;

    final @NonNull BluetoothGattService service;

    static @NonNull List<PeripheralService> wrapNativeServices(@NonNull List<BluetoothGattService> nativeServices) {
        List<PeripheralService> peripheralServices = new ArrayList<>();

        for (BluetoothGattService nativeService : nativeServices)
            peripheralServices.add(new AndroidPeripheralService(nativeService));

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

        if (!service.equals(that.service)) return false;

        return true;
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
