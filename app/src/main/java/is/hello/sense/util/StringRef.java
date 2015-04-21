package is.hello.sense.util;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A string that can be formed in one of three ways:
 * <ol>
 *     <li>From a simple string resource.</li>
 *     <li>From a string resource that has format arguments.</li>
 *     <li>From a regular String instance</li>
 * </ol>
 * This makes it possible to represent string resources,
 * and plain old strings using a single object type.
 */
public final class StringRef implements Serializable, Parcelable {
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
    private final @Nullable String string;


    //region Creation

    /**
     * Creates a string reference representing a string resource with format arguments.
     */
    public static StringRef from(@StringRes int formatRes, @NonNull Serializable... formatArgs) {
        return new StringRef(formatRes, formatArgs, null);
    }

    /**
     * Creates a string reference representing a simple string resource.
     */
    public static StringRef from(@StringRes int formatRes) {
        return new StringRef(formatRes, null, null);
    }

    /**
     * Creates a string reference representing a String instance.
     */
    public static StringRef from(@NonNull String message) {
        return new StringRef(0, null, message);
    }

    /**
     * Creates a string reference representing a formatted string.
     */
    public static StringRef from(@NonNull String format, @NonNull Object... args) {
        return new StringRef(0, null, String.format(format, (Object[]) args));
    }

    //endregion


    //region Lifecycle

    private StringRef(@StringRes int stringRes,
                      @Nullable Serializable[] formatArgs,
                      @Nullable String string) {
        this.stringRes = stringRes;
        this.formatArgs = formatArgs;
        this.string = string;
    }

    public static final Creator<StringRef> CREATOR = new Creator<StringRef>() {
        @Override
        public StringRef createFromParcel(Parcel source) {
            return new StringRef(source.readInt(),
                    (Serializable[]) source.readArray(Serializable.class.getClassLoader()),
                    source.readString());
        }

        @Override
        public StringRef[] newArray(int size) {
            return new StringRef[size];
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
        dest.writeString(string);
    }

    //endregion


    //region Resolution

    /**
     * Resolves the contents of the string reference using
     * a given context to look up string resources.
     */
    public String resolve(@NonNull Context context) {
        if (stringRes > 0) {
            if (formatArgs != null) {
                return context.getString(stringRes, (Object[]) formatArgs);
            } else {
                return context.getString(stringRes);
            }
        } else {
            return string;
        }
    }

    //endregion


    //region Identity

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringRef stringRef = (StringRef) o;

        return (stringRes == stringRef.stringRes &&
                Arrays.equals(formatArgs, stringRef.formatArgs) &&
                !(string != null ? !string.equals(stringRef.string) : stringRef.string != null));
    }

    @Override
    public int hashCode() {
        int result = stringRes;
        result = 31 * result + (formatArgs != null ? Arrays.hashCode(formatArgs) : 0);
        result = 31 * result + (string != null ? string.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (stringRes > 0) {
            if (formatArgs != null) {
                return "{StringRef res + args}";
            } else {
                return "{StringRef res}";
            }
        } else {
            return "{StringRef '" + string + "'}";
        }
    }

    //endregion
}
