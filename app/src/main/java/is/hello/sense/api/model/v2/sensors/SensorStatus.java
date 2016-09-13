package is.hello.sense.api.model.v2.sensors;

import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;

public enum SensorStatus implements Enums.FromString {
    OK,
    WAITING_FOR_DATA,
    NO_SENSE;

    public static SensorStatus fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), OK);
    }
}
