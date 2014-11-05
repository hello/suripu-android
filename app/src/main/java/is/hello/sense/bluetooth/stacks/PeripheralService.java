package is.hello.sense.bluetooth.stacks;

import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public interface PeripheralService {
    public static final int SERVICE_TYPE_PRIMARY = BluetoothGattService.SERVICE_TYPE_PRIMARY;
    public static final int SERVICE_TYPE_SECONDARY = BluetoothGattService.SERVICE_TYPE_SECONDARY;

    UUID getUuid();
    int getType();
}
