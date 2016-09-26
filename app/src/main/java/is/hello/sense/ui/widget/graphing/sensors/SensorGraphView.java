package is.hello.sense.ui.widget.graphing.sensors;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
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

    /**
     * When true a scrubber will be drawn on touch.
     */
    private boolean withScrubbing = false;
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
        if (graphDrawable == null) {
            return;
        }
        final long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime < 0) {
            postInvalidateDelayed(Math.abs(elapsedTime));
            return;
        }
        factor = (float) elapsedTime / (float) DURATION_MS;
        graphDrawable.setScaleFactor(factor);
        if (factor < 1) {
            postInvalidateDelayed(DURATION_MS / FPS);
        }


    }

    public void resetTimeToAnimate(final StartDelay delay) {
        this.startTime = System.currentTimeMillis() + delay.length;
        this.factor = 0;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (!withScrubbing) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                graphDrawable.setScrubberLocation(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                graphDrawable.setScrubberLocation(-1);
                break;
        }
        return false;
    }

    public void setSensorGraphDrawable(@NonNull final SensorGraphDrawable drawable, final boolean withScrubbing) {
        this.graphDrawable = drawable;
        this.graphDrawable.setScaleFactor(factor);
        setBackground(drawable);
        this.withScrubbing = withScrubbing;
        postInvalidate();
    }

    public enum StartDelay {
        SHORT(250),
        LONG(1500);

        private final int length;

        StartDelay(final int length) {
            this.length = length;

        }
    }

}
