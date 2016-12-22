package is.hello.sense.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

@SuppressWarnings("ResultOfMethodCallIgnored")
public abstract class SenseCache extends ValueInteractor<File> {
    private static final String TAG = SenseCache.class.getName() + ".TAG";

    private final File cache;
    private String urlLocation;
    public final InteractorSubject<File> file = this.subject;

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

    public void setUrlLocation(@NonNull final String urlLocation) {
        this.urlLocation = urlLocation;
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
        file.forget();
    }

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<File> provideUpdateObservable() {
        Log.d(TAG, "Url: " + urlLocation);
        if (urlLocation == null) {
            throw new Error("Cache UrlLocation is null");
        }
        return Observable
                .create((Observable.OnSubscribe<File>) subscriber -> {
                    Log.d(TAG, "Updating");
                    final boolean[] cancelDownload = {false};
                    subscriber.add(new Subscription() {
                        @Override
                        public void unsubscribe() {
                            cancelDownload[0] = true;
                        }

                        @Override
                        public boolean isUnsubscribed() {
                            return false;
                        }
                    });
                    String downloadFailedReason = null;
                    final File cacheFile = getCacheFile(urlLocation);
                    HttpURLConnection connection = null;
                    try{
                        final URL url = new URL(urlLocation);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.connect();
                        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            downloadFailedReason = connection.getResponseCode() + ", " + connection.getResponseMessage(); //todo change
                            return;
                        }
                        if (cacheFile.exists() && connection.getContentLength() == cacheFile.length()) {
                            Log.d(TAG, "File Already Found - Length: " + cacheFile.length());
                            return;
                        } else {
                            Log.d(TAG, "Downloading");
                        }
                        try(final InputStream input = new BufferedInputStream(url.openStream());
                            final OutputStream output = new FileOutputStream(cacheFile)){
                            final byte data[] = new byte[1024];
                            int count;
                            while ((count = input.read(data)) != -1) {
                                if (cancelDownload[0]) {
                                    return;
                                }
                                output.write(data, 0, count);
                            }
                        }

                    } catch (final IOException e) {
                        Log.d(TAG, e.toString());
                        Logger.error(TAG, e.getLocalizedMessage());
                        downloadFailedReason = e.getLocalizedMessage();
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                        if (cancelDownload[0]) {
                            cacheFile.delete();
                            subscriber.onCompleted();
                        } else if (downloadFailedReason != null) {
                            cacheFile.delete();
                            subscriber.onError(new Throwable(downloadFailedReason));
                        } else {
                            subscriber.onNext(cacheFile);
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io());
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
}
