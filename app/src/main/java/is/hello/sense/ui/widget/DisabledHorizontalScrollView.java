package is.hello.sense.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class DisabledHorizontalScrollView extends HorizontalScrollView {
    public DisabledHorizontalScrollView(Context context) {
        super(context);
    }

    public DisabledHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisabledHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
}
