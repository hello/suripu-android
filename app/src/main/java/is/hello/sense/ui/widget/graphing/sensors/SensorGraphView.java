package is.hello.sense.ui.widget.graphing.sensors;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

public class SensorGraphView extends View {
    public SensorGraphView(final Context context) {
        this(context, null);
    }

    public SensorGraphView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SensorGraphView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSensorGraphDrawable(@NonNull final SensorGraphDrawable drawable) {
        setBackground(drawable);
    }
}
