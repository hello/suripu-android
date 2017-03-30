package is.hello.sense.ui.widget.graphing.sensors;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;


/**
 * Responsible for animating {@link SensorGraphDrawable} via elapsed time.
 */
public class SensorGraphView extends View implements ValueAnimator.AnimatorUpdateListener {

    /**
     * Duration of animation in milliseconds.
     */
    private static final int DURATION_MS = 750;

    /**
     * Used for tracking elapsed time for animating.
     */
    private long startTime;

    /**
     * Ranges from 0 to 1. Graphs are drawn based on the value for smooth animation effects.
     * When 0 none of the graph is visible.
     * When 1 all of the graph is visible.
     */
    private float factor = 0;

    private SensorGraphDrawable.ScrubberCallback scrubberCallback = null;
    private SensorGraphDrawable graphDrawable;
    private final ValueAnimator animator;

    public SensorGraphView(final Context context) {
        this(context, null);
    }

    public SensorGraphView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SensorGraphView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.animator = ValueAnimator.ofFloat(0, 1);
        animator.setInterpolator(new AccelerateDecelerateInterpolator(context, attrs));
        animator.setDuration(DURATION_MS);
        animator.addUpdateListener(this);
        resetTimeToAnimate(StartDelay.SHORT);
    }

    @Override
    public void onAnimationUpdate(final ValueAnimator animation) {
        if (this.graphDrawable == null) {
            return;
        }
        this.graphDrawable.setScaleFactor(animation.getAnimatedFraction());
        this.graphDrawable.invalidateSelf();
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (this.scrubberCallback == null || graphDrawable == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                this.graphDrawable.setScrubberLocation(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                this.graphDrawable.setScrubberLocation(SensorGraphDrawable.NO_LOCATION);
                this.scrubberCallback.onScrubberReleased();
                break;
        }
        return false;
    }

    public void resetTimeToAnimate(final StartDelay delay) {
        animator.setStartDelay(delay.getLength());
        this.startTime = System.currentTimeMillis() + delay.length;
        this.factor = 0;
    }

    public synchronized void setSensorGraphDrawable(@NonNull final SensorGraphDrawable drawable) {
        this.graphDrawable = drawable;
        this.graphDrawable.setScaleFactor(this.factor);
        this.graphDrawable.setScrubberCallback(scrubberCallback);
        setBackground(this.graphDrawable);
        updateGraph();
    }

    public synchronized void setScrubberCallback(@Nullable final SensorGraphDrawable.ScrubberCallback callback) {
        this.scrubberCallback = callback;
        if (this.graphDrawable != null) {
            this.graphDrawable.setScrubberCallback(this.scrubberCallback);
        }
    }

    public void release() {
        if (this.graphDrawable != null) {
            this.graphDrawable.setScrubberCallback(null);
        }
        this.scrubberCallback = null;
    }

    /**
     * Only animates graph once on first call.
     * Subsequent calls just update graph with latest values.
     */
    public void updateGraph() {
        if (!this.animator.isStarted()
                && System.currentTimeMillis() - startTime < this.animator.getDuration()) {
            this.animator.start();
        } else {
            animator.end();
        }
    }

    public enum StartDelay {
        SHORT(250),
        LONG(1200);

        private final int length;

        StartDelay(final int length) {
            this.length = length;
        }

        public int getLength() {
            return length;
        }
    }

}
