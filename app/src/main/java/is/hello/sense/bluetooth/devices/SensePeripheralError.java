package is.hello.sense.bluetooth.devices;

import android.support.annotation.Nullable;

import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseBle;
import is.hello.sense.bluetooth.errors.BluetoothError;

public class SensePeripheralError extends BluetoothError {
    public final SenseBle.ErrorType errorType;

    public SensePeripheralError(@Nullable SenseBle.ErrorType errorType) {
        super(errorType != null ? errorType.toString() : "Unknown Sense error");

        this.errorType = SenseBle.ErrorType.INTERNAL_OPERATION_FAILED;
    }
}
