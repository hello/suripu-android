package is.hello.sense.bluetooth.devices.transmission.protobuf;

import android.support.annotation.NonNull;

import is.hello.sense.bluetooth.errors.BluetoothError;

public class ProtobufProcessingError extends BluetoothError {
    public final Reason reason;

    public ProtobufProcessingError(@NonNull Reason reason) {
        super(reason.toString());
        this.reason = reason;
    }

    public static enum Reason {
        DATA_LOST_OR_OUT_OF_ORDER("Protobuf data lost or out of order"),
        INVALID_PROTOBUF("Invalid protobuf data");

        private final String description;

        @Override
        public String toString() {
            return description;
        }

        private Reason(@NonNull String description) {
            this.description = description;
        }
    }
}
