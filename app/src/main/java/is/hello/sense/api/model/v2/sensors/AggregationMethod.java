package is.hello.sense.api.model.v2.sensors;

import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;

public enum AggregationMethod implements Enums.FromString {
    MIN,
    MAX,
    AVG,
    SUM;

    public static AggregationMethod fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), MIN);
    }
}
