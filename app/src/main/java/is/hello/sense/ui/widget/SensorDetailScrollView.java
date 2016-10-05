package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

import java.util.concurrent.atomic.AtomicLong;

import is.hello.sense.ui.widget.graphing.sensors.SensorGraphView;

/**
 * Custom {@link ScrollView} that lets the user scroll vertically or horizontally on a
 * {@link SensorGraphView}(for showing the scrubber).
 */
public class SensorDetailScrollView extends ScrollView {
    /**
     * Time to wait before activating the scrubber.
     */
    private static final int TIME_FOR_FOCUS_START_MS = 250;

    /**
     * If the user scrolls up or down by this distance we will assume they're only scrolling
     * vertically and lock the view aka suppress the scrubber.
     */
    private static final int Y_DIST_FOR_SCROLL =80;

    /**
     * Set the current time of the last {@link MotionEvent#ACTION_DOWN} event. Used for determining
     * if the user wants to scroll or scrub.
     */
    private final AtomicLong lastPress = new AtomicLong(0);

    /**
     * The Y position of the {@link MotionEvent#ACTION_DOWN} event. Used for determining if the
     * user wants to scroll or scrub.
     */
    private float initialY = 0;

    /**
     * When true the scrubber will not be shown.
     */
    private boolean lockedForScrolling = true;
    /**
     * When true we will not try checking if we should scrub or scroll anymore
     */
    private boolean skipScrubbing = false;
    /**
     * MotionEvent locations are relative to the screen. The views getY method is relative to the scrollview.
     * This will let us get the views location based on the screen.
     */
    private final int[] locationOnScreen = new int[2];

    private SensorGraphView graphView = null;

    public SensorDetailScrollView(final Context context) {
        this(context, null);
    }

    public SensorDetailScrollView(final Context context, final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SensorDetailScrollView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        if (graphView == null) {
            return super.onInterceptTouchEvent(ev);
        }

        release();
        if (ev.getAction() == MotionEvent.ACTION_DOWN && isTouchingGraphView(ev)) {
            lastPress.set(System.currentTimeMillis());
            initialY = ev.getY();
            return true;
        }
        super.onInterceptTouchEvent(ev);
        return false;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if (graphView != null) {
            if (isTouchingGraphView(ev)) {
                switch (ev.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        // If view is unlocked then the user is scrubbing. Forward touch events to graph
                        if (!lockedForScrolling) {
                            return graphView.onTouchEvent(ev);
                            // If 250 ms elapsed and we haven't skipped scrubbing yet
                        } else if (wasHeldDownForFocus() && !skipScrubbing){
                            // If the user has scrolled a lot in the Y direction assume they don't want to scrub
                            // else active scrubbing instead of scrolling
                            if (Math.abs(ev.getY() - initialY) > Y_DIST_FOR_SCROLL){
                                skipScrubbing = true;
                            }else {
                                lockedForScrolling = false;
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        release();
                        return graphView.onTouchEvent(ev);
                }
            } else {
                switch (ev.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        if (lockedForScrolling) {
                            release();
                        } else {
                            return graphView.onTouchEvent(ev);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        release();
                        graphView.onTouchEvent(ev);
                        break;
                    default:
                        release();
                }
            }

        }
        return super.onTouchEvent(ev);
    }

    public boolean isTouchingGraphView(@NonNull final MotionEvent ev) {
        graphView.getLocationOnScreen(locationOnScreen);
        final float eventY = ev.getY();
        final float graphHeight = graphView.getHeight() / 2;
        final float graphYStart = locationOnScreen[1] - graphHeight;
        final float graphYEnd = locationOnScreen[1] + graphHeight;
        return eventY > graphYStart && eventY < graphYEnd;
    }


    public boolean wasHeldDownForFocus() {
        return System.currentTimeMillis() - lastPress.get() > TIME_FOR_FOCUS_START_MS;
    }


    public void setGraphView(@Nullable final SensorGraphView graphView) {
        this.graphView = graphView;
    }

    private void release() {
        lastPress.set(System.currentTimeMillis());
        lockedForScrolling = true;
        skipScrubbing = false;
    }
}


