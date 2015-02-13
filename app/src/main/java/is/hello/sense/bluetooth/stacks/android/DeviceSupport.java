package is.hello.sense.bluetooth.stacks.android;

import android.os.Build;
import android.support.annotation.NonNull;

import is.hello.sense.bluetooth.stacks.BluetoothStack;

final class DeviceSupport {
    static @NonNull BluetoothStack.SupportLevel getDeviceSupportLevel() {
        switch (Build.MODEL) {
            case "XT1032" /* Moto G first generation */:
            case "XT1060" /* Moto X first generation */:
            case "GT-I9500" /* Samsung Galaxy S4 (Intl) */:
            case "SAMSUNG-SGH-I337" /* Samsung Galaxy S4 (AT&T) */:
            case "SM-G900" /* Galaxy S5 */:
            case "SM-N900V" /* Galaxy Note III (Verizon) */:
            case "Nexus 5" /* Nexus 5 */:
            case "Nexus 9" /* Nexus 9 */:
            case "HTC One_M8" /* HTC One M8 */:
            case "D6708" /* Sony Z3V */: {
                return BluetoothStack.SupportLevel.SUPPORTED;
            }

            case "Nexus 4" /* Nexus 4 */: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    return BluetoothStack.SupportLevel.SUPPORTED;
                } else {
                    return BluetoothStack.SupportLevel.UNSUPPORTED_OS;
                }
            }

            default: {
                return BluetoothStack.SupportLevel.UNTESTED;
            }
        }
    }
}
