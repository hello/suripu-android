package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ValueUtil {

    @NonNull
    public static String getSafeString(@Nullable final String unknownValue) {
        return getSafeValue(unknownValue, Constants.EMPTY_STRING);
    }

    @NonNull
    public static <T> List<T> getSafeList(@Nullable final List<T> list) {
        return getSafeValue(list, new ArrayList<T>());
    }

    @NonNull
    public static <T> T getSafeValue(@Nullable final T unknownValue,
                                     @NonNull final T safeValue) {
        if (unknownValue == null) {
            return safeValue;
        }
        return unknownValue;

    }
}
