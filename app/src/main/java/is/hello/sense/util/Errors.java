package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.buruberi.bluetooth.errors.BluetoothError;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.sense.api.model.ApiException;

public class Errors {
    /**
     * Returns whether or not a given exception was expected to
     * occur during normal operation of the Sense application.
     */
    public static boolean isUnexpected(@Nullable Throwable e) {
        return (e != null &&
                !(e instanceof BluetoothError) &&
                !(e instanceof ApiException));
    }

    /**
     * An error listener that records unexpected issues to analytics.
     *
     * @see Analytics#trackUnexpectedError(Throwable)
     */
    public static final ErrorListener REPORT_UNEXPECTED = e -> {
        if (Errors.isUnexpected(e)) {
            Analytics.trackUnexpectedError(e);
        }
    };

    /**
     * Returns the type string for a given error object.
     */
    public static @Nullable String getType(@Nullable Throwable e) {
        if (e != null) {
            return e.getClass().getCanonicalName();
        } else {
            return null;
        }
    }

    /**
     * Returns the context of a given error object.
     *
     * @see is.hello.buruberi.util.Errors.Reporting#getContextInfo()
     */
    public static @Nullable String getContextInfo(@Nullable Throwable e) {
        if (e != null && e instanceof Reporting) {
            return ((Reporting) e).getContextInfo();
        } else {
            return null;
        }
    }

    /**
     * Returns the human readable message for a given error object.
     *
     * @see is.hello.buruberi.util.Errors.Reporting#getDisplayMessage()
     */
    public static @Nullable StringRef getDisplayMessage(@Nullable Throwable e) {
        if (e != null) {
            if (e instanceof Reporting) {
                return ((Reporting) e).getDisplayMessage();
            } else {
                String messageString = e.getMessage();
                if (messageString != null) {
                    return StringRef.from(messageString);
                }
            }
        }

        return null;
    }

    /**
     * Describes an error with extended reporting facilities.
     */
    public interface Reporting {
        /**
         * Returns the context of an error. Meaning and form
         * depends on error, provided as a means of disambiguation.
         */
        @Nullable String getContextInfo();

        /**
         * Returns the localized message representing the
         * error's cause, and its potential resolution.
         */
        @NonNull
        StringRef getDisplayMessage();
    }


}
