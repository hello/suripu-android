package is.hello.sense.bluetooth.stacks.util;

import android.support.annotation.NonNull;

public final class BluetoothUtils {

    public static @NonNull String bytesToString(@NonNull byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);

        for (byte b : bytes)
            builder.append(String.format("%02X", b));

        return builder.toString();
    }

    public static @NonNull byte[] stringToBytes(@NonNull String string) {
        if (string.length() % 2 != 0)
            throw new IllegalArgumentException("string length is odd");

        byte[] bytes = new byte[string.length() / 2];
        for (int i = 0, length = string.length(); i < length; i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(string.substring(i, i + 2), 16);
        }
        return bytes;
    }

    public static boolean bytesStartsWith(@NonNull byte[] haystack, @NonNull byte[] needle) {
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
