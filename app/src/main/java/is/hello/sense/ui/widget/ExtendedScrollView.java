package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class ExtendedScrollView extends ScrollView {
    private boolean scrollingEnabled = true;
    private @Nullable OnScrollListener onScrollListener;


    //region Lifecycle

    public ExtendedScrollView(Context context) {
        super(context);
    }

    public ExtendedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //endregion


    //region Attributes

    public void setScrollingEnabled(boolean scrollingEnabled) {
        this.scrollingEnabled = scrollingEnabled;
    }

    public void setOnScrollListener(@Nullable OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    public View getContentView() {
        return getChildAt(0);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return super.canScrollVertically(direction);
    }

    //endregion


    //region Events

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        return scrollingEnabled && super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        return scrollingEnabled && super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onScrollChanged(int scrollX, int scrollY,
                                   int oldScrollX, int oldScrollY) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY);

        if (onScrollListener != null) {
            onScrollListener.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY);
        }
    }

    //endregion


    public interface OnScrollListener {
        void onScrollChanged(int scrollX, int scrollY,
                             int oldScrollX, int oldScrollY);
    }
}
