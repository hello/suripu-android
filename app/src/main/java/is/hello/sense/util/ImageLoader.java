package is.hello.sense.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import is.hello.buruberi.util.Rx;
import is.hello.sense.functional.Functions;
import rx.Observable;
import rx.schedulers.Schedulers;

public class ImageLoader {
    public static int SMALL = 0;
    public static int MEDIUM = 1;
    public static int LARGE = 2;

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
                        .observeOn(Rx.mainThreadScheduler());
    }

    public static Observable<Bitmap> withUrl(@NonNull String url) {
        try {
            return withUrl(new URL(url));
        } catch (MalformedURLException e) {
            return Observable.error(e);
        }
    }

    public static int getScreenDenisty(Context context) {
        switch (context.getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                return SMALL;
            case DisplayMetrics.DENSITY_MEDIUM:
                return MEDIUM;
            case DisplayMetrics.DENSITY_HIGH:
                return LARGE;
            case DisplayMetrics.DENSITY_XHIGH:
                return LARGE;
        }
        return SMALL;
    }
}
