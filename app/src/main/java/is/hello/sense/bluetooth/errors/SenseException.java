package is.hello.sense.bluetooth.errors;

import android.support.annotation.Nullable;

import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseBle;

public class SenseException extends BluetoothException {
    public final SenseBle.ErrorType errorType;

    public SenseException(@Nullable SenseBle.ErrorType errorType) {
        super(errorType != null ? errorType.toString() : "Unknown Sense error");

        this.errorType = SenseBle.ErrorType.INTERNAL_OPERATION_FAILED;
    }

    public SenseException(int errorCode) {
        this(SenseBle.ErrorType.valueOf(errorCode));
    }
}
