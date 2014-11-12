package is.hello.sense.bluetooth.stacks.android;

import android.os.Build;
import android.support.annotation.NonNull;

import is.hello.sense.bluetooth.stacks.BluetoothStack;

final class DeviceSupport {
    static @NonNull BluetoothStack.SupportLevel getSupportLevelForModel(@NonNull String model) {
        switch (model) {
            case "XT1032" /* Moto G first generation */:
                return BluetoothStack.SupportLevel.SUPPORTED;

            default:
                return BluetoothStack.SupportLevel.UNTESTED;
        }
    }

    static @NonNull BluetoothStack.SupportLevel getDeviceSupportLevel() {
        return getSupportLevelForModel(Build.MODEL);
    }
}
