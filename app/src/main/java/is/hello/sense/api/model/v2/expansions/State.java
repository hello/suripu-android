package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;

public enum State implements Enums.FromString {
    NOT_CONNECTED,
    CONNECTED_ON,
    CONNECTED_OFF,
    REVOKED,
    NOT_CONFIGURED,
    UNKNOWN;

    public static State fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), UNKNOWN);
    }
}
