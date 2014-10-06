package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Condition {
    UNKNOWN,
    IDEAL,
    WARNING,
    ALERT;

    @JsonCreator
    @SuppressWarnings("UnusedDeclaration")
    public static Condition fromString(@NonNull String value) {
        return Enums.fromString(value, values(), UNKNOWN);
    }
}
