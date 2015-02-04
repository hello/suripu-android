package is.hello.sense.bluetooth.devices;

import android.support.annotation.Nullable;

import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.errors.BluetoothError;

public class SensePeripheralError extends BluetoothError {
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
}
