package is.hello.sense.ui.common;

import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public final class ViewUtil {
    /**
     * Returns a given motion events X-coordinate, constrained to 0f or greater.
     */
    public static float getNormalizedX(@NonNull MotionEvent event) {
        return Math.max(0f, event.getX());
    }

    /**
     * Returns a given motion events Y-coordinate, constrained to 0f or greater.
     */
    public static float getNormalizedY(@NonNull MotionEvent event) {
        return Math.max(0f, event.getY());
    }

    /**
     * A continuous signal that will notify observers of a given view's global layout events.
     */
    public static <T extends View> Observable<T> onGlobalLayout(@NonNull T view) {
        return Observable.create((Observable.OnSubscribe<T>) s -> {
            ViewTreeObserver.OnGlobalLayoutListener listener = () -> s.onNext(view);
            s.add(Subscriptions.create(() -> view.getViewTreeObserver().removeOnGlobalLayoutListener(listener)));
            view.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        }).subscribeOn(AndroidSchedulers.mainThread());
    }
}
