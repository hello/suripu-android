package is.hello.sense.functional;

import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;

import is.hello.sense.util.Logger;
import rx.functions.Action1;

public final class Functions {
    public static boolean isNotNull(@Nullable Object object) {
        return (object != null);
    }
    public static boolean safeClose(@Nullable Closeable closeable) {
        if (closeable == null)
            return false;

        try {
            closeable.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    public static final Action1<Throwable> LOG_ERROR = e -> Logger.error("UnexpectedErrors", "An error occurred.", e);
}
