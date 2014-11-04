package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;

public class ProtobufProcessingException extends BluetoothException {
    public final Reason reason;

    public ProtobufProcessingException(@NonNull Reason reason) {
        super(reason.toString());
        this.reason = reason;
    }

    public static enum Reason {
        DATA_LOST_OR_OUT_OF_ORDER,
        INVALID_PROTOBUF,
    }
}
