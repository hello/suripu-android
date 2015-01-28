package is.hello.sense.bluetooth.errors;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

/**
 * Used to report errors from the gatt layer of the Android bluetooth stack.
 * <p/>
 * This error type generally should not be used outside of direct interactions
 * with a {@see is.hello.sense.bluetooth.stacks.Peripheral} object.
 */
public class BluetoothGattError extends BluetoothError {
    /**
     * This error code shows up if you turn off the Bluetooth radio,
     * and a device has an open gatt layer <em>and</em> is bonded.
     * Retrying your connection after receiving this error will work
     * seemingly 100% of the time.
     */
    public static final int STACK_ERROR = 133;

    public final int statusCode;

    public static @NonNull String statusToString(int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "GATT_SUCCESS";

            case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                return "GATT_READ_NOT_PERMITTED";

            case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                return "GATT_WRITE_NOT_PERMITTED";

            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                return "GATT_INSUFFICIENT_AUTHENTICATION";

            case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                return "GATT_REQUEST_NOT_SUPPORTED";

            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                return "GATT_INSUFFICIENT_ENCRYPTION";

            case BluetoothGatt.GATT_INVALID_OFFSET:
                return "GATT_INVALID_OFFSET";

            case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                return "GATT_INVALID_ATTRIBUTE_LENGTH";

            case BluetoothGatt.GATT_FAILURE:
                return "GATT_FAILURE";

            case -123:
            case STACK_ERROR:
                return "GATT_STACK_ERROR";

            default:
                return "UNKNOWN: " + status;
        }
    }

    public BluetoothGattError(int statusCode) {
        super(statusToString(statusCode));

        this.statusCode = statusCode;
    }

    @Override
    public boolean isFatal() {
        // If STACK_ERROR/133 is reported more than once, the gatt
        // layer is unstable, and won't be fixed until the user
        // power cycles their phone's wireless radios.
        return (statusCode == BluetoothGattError.STACK_ERROR);
    }
}
