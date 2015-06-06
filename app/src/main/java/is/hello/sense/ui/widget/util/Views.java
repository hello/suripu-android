package is.hello.sense.ui.widget.util;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

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
     * Returns whether or not a given motion event is within the bounds of a given view.
     */
    public static boolean isMotionEventInside(@NonNull View view, @NonNull MotionEvent event) {
        int[] coordinates = {0, 0};
        view.getLocationOnScreen(coordinates);

        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();

        float x = event.getRawX();
        float y = event.getRawY();

        return (x >= coordinates[0] && x <= coordinates[0] + width &&
                y >= coordinates[1] && y <= coordinates[1] + height);
    }

    public static void setBackgroundKeepingPadding(@NonNull View view, @Nullable Drawable background) {
        int paddingLeft = view.getPaddingLeft(),
            paddingTop = view.getPaddingTop(),
            paddingRight = view.getPaddingRight(),
            paddingBottom = view.getPaddingBottom();
        view.setBackground(background);
        view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    /**
     * Gets the frame of a given view within its window.
     * <p/>
     * This method makes several allocations and should
     * not be used in performance sensitive code.
     *
     * @param view      The view to find the frame for.
     * @param outRect   On return, contains the frame of the view.
     */
    public static void getFrameInWindow(@NonNull View view, @NonNull Rect outRect) {
        int[] coordinates = {0, 0};
        view.getLocationInWindow(coordinates);

        Rect windowFrame = new Rect();
        view.getWindowVisibleDisplayFrame(windowFrame);

        outRect.left = coordinates[0] - windowFrame.left;
        outRect.top = coordinates[1] - windowFrame.top;
        outRect.right = outRect.left + view.getMeasuredWidth();
        outRect.bottom = outRect.top + view.getMeasuredHeight();
    }

    public static void setFrame(@NonNull View view, @NonNull Rect rect) {
        view.setLeft(rect.left);
        view.setTop(rect.top);
        view.setRight(rect.right);
        view.setBottom(rect.bottom);
    }

    public static Rect copyFrame(@NonNull View view) {
        return new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
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


    //region Child Iterator

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

    //endregion


    public static void makeTextViewLinksClickable(@NonNull TextView textView) {
        // From <http://stackoverflow.com/questions/8558732/listview-textview-with-linkmovementmethod-makes-list-item-unclickable>
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setOnTouchListener((v, event) -> {
            Spannable spannableText = Spannable.Factory.getInstance().newSpannable(textView.getText());
            int action = event.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= textView.getTotalPaddingLeft();
                y -= textView.getTotalPaddingTop();

                x += textView.getScrollX();
                y += textView.getScrollY();

                Layout layout = textView.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] link = spannableText.getSpans(off, off, ClickableSpan.class);
                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(textView);
                    }
                    return true;
                }
            }
            return false;
        });
    }
}
