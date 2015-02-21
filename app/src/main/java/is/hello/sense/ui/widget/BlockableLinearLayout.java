package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class BlockableLinearLayout extends LinearLayout {
    private boolean touchEnabled = true;

    public BlockableLinearLayout(Context context) {
        super(context);
    }

    public BlockableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlockableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
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
