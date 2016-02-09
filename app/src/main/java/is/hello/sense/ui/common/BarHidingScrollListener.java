package is.hello.sense.ui.common;

import android.animation.ValueAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;

import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;
import is.hello.sense.ui.widget.ExtendedScrollView;

import static is.hello.sense.functional.Functions.extract;

public class BarHidingScrollListener implements ExtendedScrollView.OnScrollListener, View.OnTouchListener {
    private final Listener listener;
    private final float barHeight;
    private float hideAmount = 0f;

    private int initialScrollY = 0;
    private boolean scrollingDown = false;
    private boolean tracking = false;
    private @Nullable WeakReference<ValueAnimator> snapAnimator;

    public static void attach(@NonNull ExtendedScrollView scrollView,
                              @NonNull Listener hideListener,
                              float initialHideAmount) {
        final float barHeight = scrollView.getResources().getDimensionPixelSize(R.dimen.action_bar_height);
        final BarHidingScrollListener listener = new BarHidingScrollListener(hideListener,
                                                                             barHeight,
                                                                             initialHideAmount);
        scrollView.setOnScrollListener(listener);
        scrollView.setOnTouchListener(listener);
    }

    public BarHidingScrollListener(@NonNull Listener listener,
                                   float barHeight,
                                   float initialHideAmount) {
        this.barHeight = barHeight;
        this.listener = listener;
        this.hideAmount = initialHideAmount;
    }

    @Override
    public void onScrollChanged(int scrollX, int scrollY,
                                int oldScrollX, int oldScrollY) {
        if (tracking) {
            final int deltaY = (scrollY - initialScrollY);
            if (deltaY < 0) {
                this.hideAmount = Math.min(hideAmount, 1f - (Math.min(-deltaY, barHeight) / barHeight));
                this.scrollingDown = true;
            } else {
                this.hideAmount = Math.max(hideAmount, Math.min(deltaY, barHeight) / barHeight);
                this.scrollingDown = false;
            }

            listener.onHideAmountChanged(hideAmount);
        }
    }

    private void snap() {
        final ValueAnimator snapAnimator = extract(this.snapAnimator);
        if (snapAnimator != null) {
            snapAnimator.cancel();
        }

        final ValueAnimator newAnimator;
        if (scrollingDown) {
            newAnimator = ValueAnimator.ofFloat(hideAmount, 0f);
        } else {
            newAnimator = ValueAnimator.ofFloat(hideAmount, 1f);
        }
        AnimatorTemplate.DEFAULT.apply(newAnimator);
        newAnimator.addUpdateListener(animator -> {
            this.hideAmount = (float) animator.getAnimatedValue();
            listener.onHideAmountChanged(hideAmount);
        });
        newAnimator.start();

        this.snapAnimator = new WeakReference<>(newAnimator);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                if (!tracking) {
                    this.tracking = true;
                    this.initialScrollY = view.getScrollY();

                    final ValueAnimator snapAnimator = extract(this.snapAnimator);
                    if (snapAnimator != null) {
                        snapAnimator.cancel();
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (tracking) {
                    this.tracking = false;
                    if (hideAmount > 0f && hideAmount < 1f) {
                        snap();
                    }
                }
                break;
            }
        }
        return false;
    }

    public interface Listener {
        void onHideAmountChanged(float hideAmount);
    }
}
