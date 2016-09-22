package is.hello.sense.api.model.v2.sensors;

import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;

public enum SensorUnit implements Enums.FromString {
    CELSIUS,
    CELCIUS,
    FAHRENHEIT,
    MG_CM,
    PERCENT,
    LUX,
    DB,
    VOC,
    PPM,
    RATIO,
    KELVIN,
    KPA;

    public static SensorUnit fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), CELSIUS);
    }
}