package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;

public enum Category implements Enums.FromString {
    LIGHT,
    TEMPERATURE,
    UNKNOWN;

    public static Category fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), UNKNOWN);
    }
}
