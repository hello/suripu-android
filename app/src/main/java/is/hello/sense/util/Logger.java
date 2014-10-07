package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import retrofit.RestAdapter;

/**
 * An abstraction over the built in Android logging
 * class to allow uploading of logs to services.
 */
public class Logger {
    //region Primitive

    public static int println(int priority, @NonNull String tag, @NonNull String message) {
        Crashlytics.log(priority, tag, message);
        return Log.println(priority, tag, message);
    }

    public static String formatMessage(@NonNull String message, @Nullable Throwable e) {
        if (e != null) {
            return message + "\n" + Log.getStackTraceString(e);
        } else {
            return message;
        }
    }

    //endregion


    //region Printing

    public static int debug(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        return println(Log.DEBUG, tag, formatMessage(message, e));
    }

    public static int debug(@NonNull String tag, @NonNull String message) {
        return debug(tag, message, null);
    }


    public static int info(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        return println(Log.INFO, tag, formatMessage(message, e));
    }

    public static int info(@NonNull String tag, @NonNull String message) {
        return info(tag, message, null);
    }


    public static int warn(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        return println(Log.WARN, tag, formatMessage(message, e));
    }

    public static int warn(@NonNull String tag, @NonNull String message) {
        return warn(tag, message, null);
    }


    public static int error(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        return println(Log.ERROR, tag, formatMessage(message, e));
    }

    public static int error(@NonNull String tag, @NonNull String message) {
        return error(tag, message, null);
    }

    //endregion


    //region Utils

    public static String tagFromClass(@NonNull Class<?> clazz) {
        return clazz.getSimpleName();
    }

    public static final RestAdapter.Log RETROFIT_LOGGER = message -> Logger.debug("Retrofit", message);

    //endregion
}
