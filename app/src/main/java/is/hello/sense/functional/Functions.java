package is.hello.sense.functional;

import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;

import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.functions.Action1;
import rx.functions.Func1;

public final class Functions {

    //region General

    public static boolean safeClose(@Nullable Closeable closeable) {
        if (closeable == null) {
            return false;
        }

        try {
            closeable.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    public static int compareInts(int a, int b) {
        return (a < b) ? -1 : ((a > b) ? 1 : 0);
    }
    public static final Action1<Throwable> LOG_ERROR = e -> {
        Logger.error("UnexpectedErrors", "An error occurred.", e);
        Analytics.trackError(e, null);
    };
    public static final Func1<Boolean, Boolean> IS_TRUE = is -> is;
    public static final Func1<Boolean, Boolean> IS_FALSE = is -> !is;
    public static final Action1<Object> NO_OP = ignored -> {};
    public static final Func1<Object, Void> TO_VOID = ignored -> null;

    //endregion
}
