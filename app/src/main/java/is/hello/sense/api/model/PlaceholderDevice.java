package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import is.hello.sense.R;

public class PlaceholderDevice extends BaseDevice {
    public final Type type;

    public PlaceholderDevice(@NonNull final Type type) {
        super(State.UNKNOWN, null, null, null);

        this.type = type;
    }

    //unused
    @Override
    public int getDisplayTitleRes() {
        return R.string.device_unknown;
    }

    public enum Type {
        SENSE,
        SLEEP_PILL,
        SENSE_WITH_VOICE
    }
}
