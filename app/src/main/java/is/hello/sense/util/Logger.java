package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.json.JSONObject;

import is.hello.sense.BuildConfig;
import retrofit.RestAdapter;

/**
 * An abstraction over the built in Android logging
 * class to allow uploading of logs to services.
 */
public class Logger {
    private static final int MIN_LOGGING_LEVEL = BuildConfig.MIN_LOGGING_LEVEL;

    //region Primitive

    public static void println(int priority, @NonNull String tag, @NonNull String message) {
        if (priority >= MIN_LOGGING_LEVEL) {
            if (Crashlytics.getInstance().isInitialized()) {
                Crashlytics.log(priority, tag, message);
            }
            SessionLogger.println(priority, tag, message);
            Log.println(priority, tag, message);
        }
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

    public static void debug(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        println(Log.DEBUG, tag, formatMessage(message, e));
    }

    public static void debug(@NonNull String tag, @NonNull String message) {
        debug(tag, message, null);
    }


    public static void info(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        println(Log.INFO, tag, formatMessage(message, e));
    }

    public static void info(@NonNull String tag, @NonNull String message) {
        info(tag, message, null);
    }


    public static void warn(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        println(Log.WARN, tag, formatMessage(message, e));
    }

    public static void warn(@NonNull String tag, @NonNull String message) {
        warn(tag, message, null);
    }


    public static void error(@NonNull String tag, @NonNull String message, @Nullable Throwable e) {
        println(Log.ERROR, tag, formatMessage(message, e));
    }

    public static void error(@NonNull String tag, @NonNull String message) {
        error(tag, message, null);
    }

    public static void analytic(@NonNull String event, @Nullable JSONObject properties) {
        //noinspection ConstantConditions
        if (MIN_LOGGING_LEVEL <= Log.INFO) {
            Logger.info(Analytics.LOG_TAG, event + ": " + properties);
        } else if (Crashlytics.getInstance().isInitialized()) {
            Crashlytics.log(Log.INFO, Analytics.LOG_TAG, event);
        }
    }

    //endregion


    //region Utils

    public static final RestAdapter.Log RETROFIT_LOGGER = message -> Logger.debug("Retrofit", message);

    //endregion
}
