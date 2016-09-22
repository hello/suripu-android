package is.hello.sense.ui.widget.graphing.sensors;

import android.content.Context;

import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import android.view.MotionEvent;
import android.view.View;


/**
 * Responsible for animating {@link SensorGraphDrawable} via elapsed time.
 */
public class SensorGraphView extends View {
    /**
     * Frames per second.
     */
    private static final int FPS = 60;
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

    public SensorGraphView(final Context context) {
        this(context, null);
    }

    public SensorGraphView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SensorGraphView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        resetTimeToAnimate(StartDelay.SHORT);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (this.graphDrawable == null) {
            return;
        }
        final long elapsedTime = System.currentTimeMillis() - this.startTime;
        if (elapsedTime < 0) {
            postInvalidateDelayed(Math.abs(elapsedTime));
            return;
        }
        this.factor = (float) elapsedTime / (float) DURATION_MS;
        this.graphDrawable.setScaleFactor(this.factor);
        if (this.factor < 1) {
            postInvalidateDelayed(DURATION_MS / FPS);
        }


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
                this.graphDrawable.setScrubberLocation(-1);
                this.scrubberCallback.onScrubberReleased();
                break;
        }
        return false;
    }

    public void resetTimeToAnimate(final StartDelay delay) {
        this.startTime = System.currentTimeMillis() + delay.length;
        this.factor = 0;
    }

    public synchronized void setSensorGraphDrawable(@NonNull final SensorGraphDrawable drawable) {
        this.graphDrawable = drawable;
        this.graphDrawable.setScaleFactor(this.factor);
        this.graphDrawable.setScrubberCallback(scrubberCallback);
        setBackground(this.graphDrawable);
        postInvalidate();
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


    public enum StartDelay {
        SHORT(250),
        LONG(1500);

        private final int length;

        StartDelay(final int length) {
            this.length = length;
        }

        public int getLength() {
            return length;
        }
    }

}
