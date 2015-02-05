package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class BlockableScrollView extends ScrollView {
    private boolean scrollingEnabled = true;

    public BlockableScrollView(Context context) {
        super(context);
    }

    public BlockableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlockableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void setScrollingEnabled(boolean scrollingEnabled) {
        this.scrollingEnabled = scrollingEnabled;
    }


    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        return scrollingEnabled && super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        return scrollingEnabled && super.onInterceptTouchEvent(ev);
    }
}
