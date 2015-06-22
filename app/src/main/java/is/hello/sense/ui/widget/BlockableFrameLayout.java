package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class BlockableFrameLayout extends FrameLayout {
    private boolean touchEnabled = true;

    public BlockableFrameLayout(Context context) {
        super(context);
    }

    public BlockableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlockableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void setTouchEnabled(boolean touchEnabled) {
        this.touchEnabled = touchEnabled;
    }


    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        return touchEnabled || super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        return !touchEnabled || super.onInterceptTouchEvent(ev);
    }
}
