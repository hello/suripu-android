package is.hello.sense.api.model;

import android.support.annotation.NonNull;

public class PlaceholderDevice extends BaseDevice {
    public final Type type;

    public PlaceholderDevice(@NonNull Type type) {
        super(State.UNKNOWN, null, null, null);

        this.type = type;
    }

    public enum Type {
        SENSE,
        SLEEP_PILL,
    }
}
