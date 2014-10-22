package is.hello.sense.functional;

import android.support.annotation.Nullable;

import is.hello.sense.util.Logger;
import rx.functions.Action1;

public final class Functions {
    public static boolean isNotNull(@Nullable Object object) {
        return (object != null);
    }
    public static final Action1<Throwable> IGNORE_ERROR = ignored -> {};
    public static final Action1<Throwable> LOG_ERROR = e -> Logger.error("UnexpectedErrors", "An error occurred.", e);
}
