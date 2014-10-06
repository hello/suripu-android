package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class Enums {
    public static <T extends Enum<T>> T fromString(@Nullable String value, T[] values, @NonNull T unknown) {
        if (!TextUtils.isEmpty(value)) {
            for (T possibleMatch : values) {
                if (possibleMatch.name().equals(value))
                    return possibleMatch;
            }
        }

        return unknown;
    }
}
