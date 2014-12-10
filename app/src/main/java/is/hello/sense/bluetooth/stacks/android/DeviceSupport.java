package is.hello.sense.bluetooth.stacks.android;

import android.os.Build;
import android.support.annotation.NonNull;

import is.hello.sense.bluetooth.stacks.BluetoothStack;

final class DeviceSupport {
    static @NonNull BluetoothStack.SupportLevel getSupportLevelForModel(@NonNull String model) {
        switch (model) {
            case "XT1032" /* Moto G first generation */:
            case "GT-I9500" /* Samsung Galaxy S4 (Intl) */:
            case "HTC One_M8" /* HTC One M8 */:
            case "Nexus 4" /* Nexus 4 */:
                return BluetoothStack.SupportLevel.SUPPORTED;

            default:
                return BluetoothStack.SupportLevel.UNTESTED;
        }
    }

    static @NonNull BluetoothStack.SupportLevel getDeviceSupportLevel() {
        return getSupportLevelForModel(Build.MODEL);
    }
}
