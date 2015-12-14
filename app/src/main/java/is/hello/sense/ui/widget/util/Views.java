package is.hello.sense.ui.widget.util;

import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.util.SafeOnClickListener;
import is.hello.sense.util.StateSafeExecutor;

public final class Views {
    /**
     * For use with {@link View#getLocationOnScreen(int[])}
     * and {@link View#getLocationInWindow(int[])}.
     */
    public static final int ORIGIN_X = 0;

    /**
     * For use with {@link View#getLocationOnScreen(int[])}
     * and {@link View#getLocationInWindow(int[])}.
     */
    public static final int ORIGIN_Y = 1;


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

        return (x >= coordinates[ORIGIN_X] && x <= coordinates[ORIGIN_X] + width &&
                y >= coordinates[ORIGIN_Y] && y <= coordinates[ORIGIN_Y] + height);
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

        outRect.left = coordinates[ORIGIN_X] - windowFrame.left;
        outRect.top = coordinates[ORIGIN_Y] - windowFrame.top;
        outRect.right = outRect.left + view.getMeasuredWidth();
        outRect.bottom = outRect.top + view.getMeasuredHeight();
    }

    public static Rect copyFrame(@NonNull View view) {
        return new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
    }

    /**
     * Calculates the center X coordinate for a given View instance.
     * @param view  The view to find the center X coordinate for.
     * @return  The center X coordinate of the view, or <code>0f</code> if the view is not yet laid out.
     */
    public static int getCenterX(@NonNull View view) {
        return (view.getLeft() + view.getRight()) / 2;
    }

    /**
     * Executes a runnable when a given view has been laid out.
     * <p>
     * If the view is laid out at call time, the runnable will be immediately executed.
     * If the view is in the process of laying out, the runnable will be executed on the
     * next looper cycle. Otherwise, a layout listener is attached to the view and the
     * runnable will be executed when the listener receives a callback.
     *
     * @param view      The view that needs to be laid out before the runnable can be run.
     * @param runnable  The runnable to execute when the view is laid out.
     */
    public static void runWhenLaidOut(@NonNull View view, @NonNull Runnable runnable) {
        if (ViewCompat.isLaidOut(view)) {
            runnable.run();
        } else if (view.isInLayout()) {
            view.post(runnable);
        } else {
            final ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    runnable.run();
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            };
            view.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        }
    }

    public static void setSafeOnClickListener(@NonNull View view, @NonNull View.OnClickListener onClickListener) {
        view.setOnClickListener(new SafeOnClickListener(null, onClickListener));
    }

    public static void setSafeOnClickListener(@NonNull View view,
                                              @Nullable StateSafeExecutor stateSafeExecutor,
                                              @NonNull View.OnClickListener onClickListener) {
        view.setOnClickListener(new SafeOnClickListener(stateSafeExecutor, onClickListener));
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

    public static ValueAnimator createFrameAnimator(@NonNull View view, @NonNull Rect... frames) {
        final ValueAnimator animator = AnimatorTemplate.DEFAULT.createRectAnimator((Rect[]) frames);
        animator.addUpdateListener(a -> {
            final Rect frame = (Rect) a.getAnimatedValue();
            view.layout(frame.left, frame.top, frame.right, frame.bottom);
        });
        return animator;
    }
}
