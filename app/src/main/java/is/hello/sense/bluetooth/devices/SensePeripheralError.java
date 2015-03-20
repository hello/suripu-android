package is.hello.sense.bluetooth.devices;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.util.Errors;

public class SensePeripheralError extends BluetoothError implements Errors.Reporting {
    public final SenseCommandProtos.ErrorType errorType;

    public SensePeripheralError(@Nullable SenseCommandProtos.ErrorType errorType, @Nullable Throwable cause) {
        super(errorType != null ? errorType.toString() : "Unknown Sense error", cause);

        this.errorType = errorType != null ? errorType : SenseCommandProtos.ErrorType.INTERNAL_OPERATION_FAILED;
    }

    public SensePeripheralError(@Nullable SenseCommandProtos.ErrorType errorType) {
        this(errorType, null);
    }

    public static boolean errorTypeEquals(@Nullable Throwable error, SenseCommandProtos.ErrorType errorType) {
        return (error != null && (error instanceof SensePeripheralError) &&
                ((SensePeripheralError) error).errorType == errorType);

    }

    @Nullable
    @Override
    public String getContextInfo() {
        return errorType.toString();
    }

    @NonNull
    @Override
    public Errors.Message getDisplayMessage() {
        switch (errorType) {
            case DEVICE_ALREADY_PAIRED:
                return Errors.Message.from(R.string.error_sense_already_paired);

            case DEVICE_DATABASE_FULL:
                return Errors.Message.from(R.string.error_sense_device_db_full);

            case TIME_OUT:
                return Errors.Message.from(R.string.error_generic_bluetooth_timeout);

            case NETWORK_ERROR:
                return Errors.Message.from(R.string.error_network_failure);

            case WLAN_CONNECTION_ERROR:
            case NO_ENDPOINT_IN_RANGE:
                return Errors.Message.from(R.string.error_wifi_connection_failed);

            case FAIL_TO_OBTAIN_IP:
                return Errors.Message.from(R.string.error_wifi_ip_failure);

            case INTERNAL_DATA_ERROR:
            case DEVICE_NO_MEMORY:
            case INTERNAL_OPERATION_FAILED:
                return Errors.Message.from(R.string.error_generic_sense_failure);

            default:
                return Errors.Message.from(errorType.toString());
        }
    }
}
