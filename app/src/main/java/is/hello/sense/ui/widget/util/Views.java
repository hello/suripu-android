package is.hello.sense.ui.widget.util;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

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

    public static Rect copyFrame(@NonNull View view) {
        return new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
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

    public static void setSafeOnClickListener(@NonNull View view, @NonNull View.OnClickListener onClickListener) {
        view.setOnClickListener(new SafeOnClickListener(onClickListener));
    }


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
