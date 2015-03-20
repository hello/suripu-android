package is.hello.sense.util;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import java.io.Serializable;

import is.hello.sense.SenseApplication;

public class Errors {
    public static final String LOG_TAG = "UnexpectedErrors";

    /**
     * Logs a given error object to the console, and to Analytics.
     *
     * @see is.hello.sense.functional.Functions#LOG_ERROR The global lambda.
     */
    public static void logError(@Nullable Throwable e) {
        Logger.error(LOG_TAG, "An error occurred.", e);

        Message message = getDisplayMessage(e);
        String messageString;
        if (message != null) {
            messageString = message.resolve(SenseApplication.getInstance());
        } else {
            messageString = "Unknown";
        }
        Analytics.trackError(messageString, getType(e), getContextInfo(e), null);
    }

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
     * @see is.hello.sense.util.Errors.Reporting#getContextInfo()
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
     * @see is.hello.sense.util.Errors.Reporting#getDisplayMessage()
     */
    public static @Nullable Message getDisplayMessage(@Nullable Throwable e) {
        if (e != null) {
            if (e instanceof Reporting) {
                return ((Reporting) e).getDisplayMessage();
            } else {
                String messageString = e.getMessage();
                if (messageString != null) {
                    return Message.from(messageString);
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
        @NonNull Message getDisplayMessage();
    }


    /**
     * A string that can be formed in one of three ways:
     * <ol>
     *     <li>From a simple string resource.</li>
     *     <li>From a string resource that has format arguments.</li>
     *     <li>From a regular String instance</li>
     * </ol>
     * This makes it possible to represent localized messages,
     * and fallback strings in a single return value.
     * <p/>
     * Always prefer parceling over serialization for Message if possible.
     */
    public static final class Message implements Serializable, Parcelable {
        /**
         * A string resource that may or may not represent a format.
         * <p/>
         * A zero-value indicates the Message contains a String instance.
         */
        private final @StringRes int stringRes;

        /**
         * The arguments that go with the string resource, if any.
         */
        private final @Nullable Serializable[] formatArgs;

        /**
         * A String instance. Should never by null in practice.
         */
        private final @Nullable String message;


        //region Creation

        /**
         * Creates a message representing a string resource with format arguments.
         */
        public static Message from(@StringRes int formatRes, Serializable... formatArgs) {
            return new Message(formatRes, formatArgs, null);
        }

        /**
         * Creates a message representing a simple string resource.
         */
        public static Message from(@StringRes int formatRes) {
            return new Message(formatRes, null, null);
        }

        /**
         * Creates a message representing a String instance.
         */
        public static Message from(@NonNull String message) {
            return new Message(0, null, message);
        }

        //endregion


        //region Lifecycle

        private Message(@StringRes int stringRes,
                        @Nullable Serializable[] formatArgs,
                        @Nullable String message) {
            this.stringRes = stringRes;
            this.formatArgs = formatArgs;
            this.message = message;
        }

        public static final Creator<Message> CREATOR = new Creator<Message>() {
            @Override
            public Message createFromParcel(Parcel source) {
                return new Message(source.readInt(),
                        (Serializable[]) source.readArray(Serializable.class.getClassLoader()),
                        source.readString());
            }

            @Override
            public Message[] newArray(int size) {
                return new Message[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(stringRes);
            dest.writeArray(formatArgs);
            dest.writeString(message);
        }

        //endregion


        //region Resolution

        /**
         * Resolves the contents of the Message using a
         * given context to look up string resources.
         */
        public String resolve(@NonNull Context context) {
            if (stringRes > 0) {
                if (formatArgs != null) {
                    return context.getString(stringRes, (Object[]) formatArgs);
                } else {
                    return context.getString(stringRes);
                }
            } else {
                return message;
            }
        }

        //endregion
    }
}
