package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Errors {
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
     * @see is.hello.sense.util.Errors.Reporting#getContext()
     */
    public static @Nullable String getContext(@Nullable Throwable e) {
        if (e != null && e instanceof Reporting) {
            return ((Reporting) e).getContext();
        } else {
            return null;
        }
    }

    public interface Reporting {
        /**
         * Returns the context of an error. Meaning and form
         * depends on error, provided as a means of disambiguation.
         */
        @Nullable String getContext();
    }
}
