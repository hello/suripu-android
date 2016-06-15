package is.hello.sense.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.File;

public abstract class SenseCache {

    private final File cache;

    SenseCache(@NonNull final Context context, @NonNull final String directoryName) {
        if (directoryName.isEmpty() || directoryName.charAt(0) != '/') {
            throw new Error("Invalid cache directory");
        }
        cache = new File(context.getCacheDir() + directoryName);
        if (!cache.isDirectory()) {
            if (!cache.mkdir()) {
                throw new Error("Failed to create directory");
            }
        }
    }


    public File getCacheFile(@NonNull final String url) {
        final String fileName = Uri.parse(url).getLastPathSegment();
        final File file = new File(cache, fileName);
        file.deleteOnExit();
        return file;
    }

    public void trimCache() {
        for (final String child : cache.list()) {
            new File(cache, child).delete();
        }
    }

    public static class AudioCache extends SenseCache {
        public AudioCache(@NonNull final Context context) {
            super(context, "/audio");
        }
    }

    public static class ImageCache extends SenseCache {

        public ImageCache(@NonNull final Context context) {
            super(context, "/image");
        }
    }

}
