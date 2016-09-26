package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
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
    private static final int TIME_FOR_FOCUS = 200; //ms

    /**
     * If the user scrolls up or down by this distance we will assume they're only scrolling
     * vertically and lock the view aka suppress the scrubber.
     */
    private static final int Y_DIST_FOR_SCROLL = 100;

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
    private boolean locked = false;
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
        switch (ev.getAction()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                lastPress.set(System.currentTimeMillis());
                initialY = ev.getY();
                return true;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        if (graphView != null) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    if (wasHeldDownForFocus() && !locked) {
                        return graphView.onTouchEvent(ev);
                    } else if (Math.abs(ev.getY() - initialY) > Y_DIST_FOR_SCROLL) {
                        locked = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    lastPress.set(System.currentTimeMillis());
                    locked = false;
                    return graphView.onTouchEvent(ev);
            }

        }
        return super.onTouchEvent(ev);
    }

    public boolean wasHeldDownForFocus() {
        return System.currentTimeMillis() - lastPress.get() > TIME_FOR_FOCUS;
    }


    public void setGraphView(@NonNull final SensorGraphView graphView) {
        this.graphView = graphView;
    }
}


