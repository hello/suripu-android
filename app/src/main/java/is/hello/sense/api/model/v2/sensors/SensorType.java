package is.hello.sense.api.model.v2.sensors;


import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;

public enum SensorType implements Enums.FromString {
    TEMPERATURE,
    HUMIDITY,
    LIGHT,
    PARTICULATES,
    SOUND,
    CO2,
    TVOC,
    LIGHT_TEMPERATURE,
    UV,
    UNKNOWN;

    public static SensorType fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), UNKNOWN);
    }
}
