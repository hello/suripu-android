package is.hello.sense.ui.widget;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.Iterator;
import java.util.NoSuchElementException;

import is.hello.sense.util.SafeOnClickListener;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public final class Views {
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
     * Searches the children of a given view group for a child that contains a given y
     * coordinate; returns null if no matching child could be found.
     * <p/>
     * <em>Important:</em> This method does not take transformations into account.
     */
    public static @Nullable View findChildAtY(@NonNull ViewGroup view, float y) {
        for (int i = 0, count = view.getChildCount(); i < count; i++) {
            View child = view.getChildAt(i);
            if (y >= child.getTop() && y <= child.getBottom()) {
                return child;
            }
        }

        return null;
    }

    /**
     * A one time signal that will notify observers of a given view's next global layout event.
     */
    public static <T extends View> Observable<T> observeNextLayout(@NonNull T view) {
        return Observable.create((Observable.OnSubscribe<T>) s -> {
            ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    s.onNext(view);
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            };
            s.add(Subscriptions.create(() -> view.getViewTreeObserver().removeOnGlobalLayoutListener(listener)));
            view.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        }).subscribeOn(AndroidSchedulers.mainThread());
    }


    public static Iterable<View> children(@NonNull ViewGroup view) {
        return new Iterable<View>() {
            @Override
            public Iterator<View> iterator() {
                return new ChildIterator(view);
            }
        };
    }

    public static class ChildIterator implements Iterator<View> {
        private final ViewGroup view;
        private int pointer = 0;

        public ChildIterator(@NonNull ViewGroup view) {
            this.view = view;
        }

        @Override
        public boolean hasNext() {
            return (pointer < view.getChildCount());
        }

        @Override
        public View next() {
            if (hasNext()) {
                return view.getChildAt(pointer++);
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            view.removeViewAt(--pointer);
        }
    }


    public static void setSafeOnClickListener(@NonNull View view, @NonNull View.OnClickListener onClickListener) {
        view.setOnClickListener(new SafeOnClickListener(onClickListener));
    }
}
