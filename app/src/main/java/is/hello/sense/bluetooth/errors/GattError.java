package is.hello.sense.bluetooth.errors;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

public class GattError extends BluetoothError {
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

            default:
                return "UNKNOWN: " + status;
        }
    }

    public GattError(int status) {
        super(statusToString(status));

        this.statusCode = status;
    }
}
