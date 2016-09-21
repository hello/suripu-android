package is.hello.sense.ui.widget.graphing.sensors;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
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

    private final long startTime;

    private SensorGraphDrawable graphDrawable;
    private float factor = 0;

    public SensorGraphView(final Context context) {
        this(context, null);
    }

    public SensorGraphView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SensorGraphView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (graphDrawable == null) {
            return;
        }
        factor = (System.currentTimeMillis() - startTime) / (float) DURATION_MS;
        graphDrawable.setScaleFactor(factor);
        if (factor < 1) {
            postInvalidateDelayed(DURATION_MS / FPS);
        }


    }

    public void setSensorGraphDrawable(@NonNull final SensorGraphDrawable drawable) {
        this.graphDrawable = drawable;
        this.graphDrawable.setScaleFactor(factor);
        setBackground(drawable);
    }
}
