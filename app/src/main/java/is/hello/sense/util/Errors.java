package is.hello.sense.util;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import java.io.Serializable;

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

    public static final class Message implements Serializable, Parcelable {
        private final @StringRes int formatRes;
        private final @Nullable Serializable[] formatArgs;
        private final @Nullable String message;


        //region Creation

        public static Message from(@StringRes int formatRes, Serializable... formatArgs) {
            return new Message(formatRes, formatArgs, null);
        }

        public static Message from(@StringRes int formatRes) {
            return new Message(formatRes, null, null);
        }

        public static Message from(@NonNull String message) {
            return new Message(0, null, message);
        }

        //endregion


        //region Lifecycle

        private Message(@StringRes int formatRes,
                        @Nullable Serializable[] formatArgs,
                        @Nullable String message) {
            this.formatRes = formatRes;
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
            dest.writeInt(formatRes);
            dest.writeArray(formatArgs);
            dest.writeString(message);
        }

        //endregion


        //region Resolution

        public String resolve(@NonNull Context context) {
            if (formatRes > 0) {
                if (formatArgs != null) {
                    return context.getString(formatRes, (Object[]) formatArgs);
                } else {
                    return context.getString(formatRes);
                }
            } else {
                return message;
            }
        }

        //endregion
    }
}
