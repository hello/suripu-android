package is.hello.sense.functional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class Functions {
    public static boolean isNotNull(@Nullable Object object) {
        return (object != null);
    }
    public static void ignoreError(@NonNull Throwable e) {}
}
