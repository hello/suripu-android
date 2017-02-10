package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import is.hello.sense.R;

public class PlaceholderDevice extends BaseDevice {
    public final Type type;
    private boolean collapsed;

    public PlaceholderDevice(@NonNull final Type type) {
        super(State.UNKNOWN, null, null, null);

        this.type = type;
        this.collapsed = false;
    }

    //unused
    @Override
    public int getDisplayTitleRes() {
        return R.string.device_unknown;
    }

    /**
     * @return new collapsed boolean state
     */
    public boolean toggleCollapsed() {
        this.collapsed = !collapsed;
        return collapsed;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public enum Type {
        SENSE,
        SLEEP_PILL,
        SENSE_WITH_VOICE
    }
}
