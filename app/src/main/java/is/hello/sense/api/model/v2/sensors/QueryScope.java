package is.hello.sense.api.model.v2.sensors;

import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;

public enum QueryScope implements Enums.FromString {

    DAY_5_MINUTE,
    LAST_3H_5_MINUTE,
    WEEK_1_HOUR;


    public static QueryScope fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), DAY_5_MINUTE);
    }

}
