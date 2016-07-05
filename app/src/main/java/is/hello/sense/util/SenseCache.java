package is.hello.sense.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("ResultOfMethodCallIgnored")
public abstract class SenseCache {
    private static final String TAG = SenseCache.class.getName() + ".TAG";

    private final File cache;
    private FileDownloadThread fileDownloadThread = null;

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

    public void cancelDownload() {
        if (fileDownloadThread != null) {
            fileDownloadThread.cancelDownload();
        }
    }

    public File getCacheFile(@NonNull final String urlLocation) {
        final String fileName = Uri.parse(urlLocation).getLastPathSegment();
        final File file = new File(cache, fileName);
        file.deleteOnExit();
        return file;
    }

    public void trimCache() {
        for (final String child : cache.list()) {
            new File(cache, child).delete();
        }
    }

    public void downloadFile(@NonNull final String url, @NonNull final DownloadListener downloadListener) {
        if (fileDownloadThread != null) {
            fileDownloadThread.cancelDownload();
        }
        fileDownloadThread = new FileDownloadThread(url, downloadListener);
        fileDownloadThread.start();
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

    public static class FirmwareCache extends SenseCache {

        public FirmwareCache(@NonNull final Context context) {
            super(context, "/firmware");
        }
    }

    public interface DownloadListener {

        void onDownloadCompleted(@NonNull final File file);

        void onDownloadFailed(@Nullable final String reason);

        void onDownloadCanceled();

    }


    private class FileDownloadThread extends Thread {
        private boolean cancelDownload = false;
        private final String url;
        private final DownloadListener downloadListener;

        private FileDownloadThread(@NonNull final String url, final DownloadListener downloadListener) {
            this.url = url;
            this.downloadListener = downloadListener;
        }

        private void cancelDownload() {
            this.cancelDownload = true;
        }


        @Override
        public void run() {
            String downloadFailedReason = null;
            final File cacheFile = getCacheFile(url);
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                final URL url = new URL(this.url);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    downloadFailedReason = connection.getResponseCode() + ", " + connection.getResponseMessage(); //todo change
                    return;
                }
                input = new BufferedInputStream(url.openStream(), 8192);
                output = new FileOutputStream(cacheFile);
                final byte data[] = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    if (cancelDownload) {
                        return;
                    }
                    output.write(data, 0, count);
                }
            } catch (final IOException e) {
                Logger.error(TAG, e.getLocalizedMessage());
                downloadFailedReason = e.getLocalizedMessage();
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
                } catch (final IOException e) {
                    Logger.error(TAG, e.getLocalizedMessage());
                }
                if (connection != null) {
                    connection.disconnect();
                }
                if (cancelDownload) {
                    cacheFile.delete();
                    downloadListener.onDownloadCanceled();
                } else if (downloadFailedReason != null) {
                    cacheFile.delete();
                    downloadListener.onDownloadFailed(downloadFailedReason);
                } else {
                    downloadListener.onDownloadCompleted(cacheFile);
                }
                fileDownloadThread = null;
            }
        }
    }
}
