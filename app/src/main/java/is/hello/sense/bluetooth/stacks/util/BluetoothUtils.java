package is.hello.sense.bluetooth.stacks.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public final class BluetoothUtils {

    /**
     * Converts an array of bytes to a string of the format <code>0122FF</code>
     */
    public static @NonNull String convertBytesToString(@NonNull byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            builder.append(String.format("%02X", b));
        }

        return builder.toString();
    }

    /**
     * Converts a string of the format <code>0122FF</code> to an array of bytes.
     *
     * @throws java.lang.IllegalArgumentException on invalid input.
     */
    public static @NonNull byte[] convertStringToBytes(@Nullable String string) {
        if (TextUtils.isEmpty(string)) {
            return new byte[0];
        }

        if (string.length() % 2 != 0) {
            throw new IllegalArgumentException("string length is odd");
        }

        byte[] bytes = new byte[string.length() / 2];
        for (int i = 0, length = string.length(); i < length; i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(string.substring(i, i + 2), 16);
        }
        return bytes;
    }

    /**
     * Converts a string of the format <code>0122FF</code> to an array of bytes.
     * <p/>
     * The same as {@see #convertStringToBytes(String)}, but it does not throw exceptions.
     *
     * @return A <code>byte[]</code> array if the string could be converted; null otherwise.
     */
    public static @Nullable byte[] tryConvertStringToBytes(@Nullable String string) {
        try {
            return convertStringToBytes(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean bytesStartWith(@NonNull byte[] haystack, @NonNull byte[] needle) {
        if (haystack.length < needle.length) {
            return false;
        }

        for (int i = 0, length = needle.length; i < length; i++) {
            if (haystack[i] != needle[i]) {
                return false;
            }
        }

        return true;
    }

}
