package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class ExtendedScrollView extends ScrollView {
    private boolean scrollingEnabled = true;
    private @Nullable OnScrollListener onScrollListener;
    private boolean dispatchedInitialScrollEvent = false;


    //region Lifecycle

    public ExtendedScrollView(@NonNull Context context) {
        super(context);
    }

    public ExtendedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //endregion


    //region Attributes

    public void setScrollingEnabled(boolean scrollingEnabled) {
        this.scrollingEnabled = scrollingEnabled;
    }

    public void setOnScrollListener(@Nullable OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;

        if (onScrollListener != null && ViewCompat.isLaidOut(this)) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            onScrollListener.onScrollChanged(scrollX, scrollY,
                                             scrollX, scrollY);

            this.dispatchedInitialScrollEvent = true;
        } else {
            this.dispatchedInitialScrollEvent = false;
        }
    }

    public View getContentView() {
        return getChildAt(0);
    }

    //endregion


    //region Events

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (!dispatchedInitialScrollEvent) {
            if (onScrollListener != null) {
                final int scrollX = getScrollX();
                final int scrollY = getScrollY();
                onScrollListener.onScrollChanged(scrollX, scrollY,
                                                 scrollX, scrollY);
            }
            
            this.dispatchedInitialScrollEvent = true;
        }
    }

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
