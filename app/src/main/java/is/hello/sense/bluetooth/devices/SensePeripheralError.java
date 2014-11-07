package is.hello.sense.bluetooth.devices;

import android.support.annotation.Nullable;

import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.bluetooth.errors.BluetoothError;

public class SensePeripheralError extends BluetoothError {
    public final MorpheusBle.ErrorType errorType;

    public SensePeripheralError(@Nullable MorpheusBle.ErrorType errorType) {
        super(errorType != null ? errorType.toString() : "Unknown Sense error");

        this.errorType = errorType != null ? errorType : MorpheusBle.ErrorType.INTERNAL_OPERATION_FAILED;
    }
}
