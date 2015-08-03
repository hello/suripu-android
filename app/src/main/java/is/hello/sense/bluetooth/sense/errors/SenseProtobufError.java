package is.hello.sense.bluetooth.sense.errors;

import android.support.annotation.NonNull;

import is.hello.buruberi.bluetooth.errors.BluetoothError;

public class SenseProtobufError extends BluetoothError {
    public final Reason reason;

    public SenseProtobufError(@NonNull Reason reason) {
        super(reason.toString());
        this.reason = reason;
    }

    public enum Reason {
        DATA_LOST_OR_OUT_OF_ORDER("Protobuf data lost or out of order"),
        INVALID_PROTOBUF("Invalid protobuf data");

        private final String description;

        @Override
        public String toString() {
            return description;
        }

        Reason(@NonNull String description) {
            this.description = description;
        }
    }
}
