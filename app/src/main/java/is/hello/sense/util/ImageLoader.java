package is.hello.sense.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import is.hello.sense.functional.Functions;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ImageLoader {
    public static Observable<Bitmap> withUrl(@NonNull URL url) {
        Observable<Bitmap> operation = Observable.create(s -> {
            InputStream stream = null;
            try {
                Logger.info(ImageLoader.class.getSimpleName(), "<--- " + url);
                stream = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                Logger.info(ImageLoader.class.getSimpleName(), "---> " + url);
                s.onNext(bitmap);
                s.onCompleted();
            } catch (Exception e) {
                Logger.error(ImageLoader.class.getSimpleName(), "---> " + url, e);
                s.onError(e);
            } finally {
                Functions.safeClose(stream);
            }
        });

        return operation.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<Bitmap> withUrl(@NonNull String url) {
        try {
            return withUrl(new URL(url));
        } catch (MalformedURLException e) {
            return Observable.error(e);
        }
    }
}
