package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

public class PageView extends FrameLayout {
    private PageAdapter pageAdapter;
    private View currentView;

    public PageView(Context context) {
        super(context);
        initialize();
    }

    public PageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }


    //region Properties

    public PageAdapter getPageAdapter() {
        return pageAdapter;
    }

    public void setPageAdapter(PageAdapter pageAdapter) {
        this.pageAdapter = pageAdapter;
    }

    public View getCurrentView() {
        return currentView;
    }

    public void setCurrentView(View currentView) {
        if (currentView != null) {
            removeView(currentView);
        }

        this.currentView = currentView;

        if (currentView != null) {
            addView(currentView);
        }
    }

    //endregion


    //region Events

    private int touchSlop;
    private int viewWidth;
    private float initialX, initialViewX;
    private boolean touchEventsTracked = false;
    private boolean hasViewBefore = false, hasViewAfter = false;

    private int transitionDirection;
    private View transitioningView;
    private float transitioningViewX;

    private void injectViewForPosition(@NonNull Side side) {
        switch (side) {
            case LEFT: {
                if (transitioningView == null) {
                    transitioningView = getPageAdapter().getViewBeforeView(currentView);
                    addView(transitioningView);
                }
                this.transitioningViewX = currentView.getX() - viewWidth;
                break;
            }


            case RIGHT: {
                if (transitioningView == null) {
                    transitioningView = getPageAdapter().getViewAfterView(currentView);
                    addView(transitioningView);
                }
                this.transitioningViewX = currentView.getX() + viewWidth;
                break;
            }
        }

        transitioningView.setX(transitioningViewX);
    }

    private void completeTransition(@NonNull Side side) {

    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (currentView == null || pageAdapter == null)
            return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.initialX = event.getX();
                this.initialViewX = currentView.getX();
                this.viewWidth = currentView.getMeasuredWidth();

                this.hasViewBefore = getPageAdapter().hasViewBeforeView(currentView);
                this.hasViewAfter = getPageAdapter().hasViewAfterView(currentView);

                Log.i("events", "touch down");

                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                float x = event.getX();
                float deltaX = x - initialX;
                if (Math.abs(deltaX) > touchSlop) {
                    this.touchEventsTracked = true;
                }

                Log.i("events", "touch moved; deltaX: " + deltaX + "; touchEventsTracked: " + touchEventsTracked);

                if (touchEventsTracked) {
                    float newX = initialViewX + deltaX;
                    if (hasViewBefore) {
                        currentView.setX(newX);
                        injectViewForPosition(Side.LEFT);
                    }

                    if (hasViewAfter) {
                        currentView.setX(newX);
                        injectViewForPosition(Side.RIGHT);
                    }

                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_UP: {
                Log.i("events", "touch up");

                if (touchEventsTracked) {
                    this.touchEventsTracked = false;

                    return true;
                }

                break;
            }
        }

        return false;
    }


    //endregion


    protected void initialize() {
        this.touchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
    }


    public interface PageAdapter {
        boolean hasViewBeforeView(@NonNull View view);
        @NonNull View getViewBeforeView(@NonNull View view);

        boolean hasViewAfterView(@NonNull View view);
        @NonNull View getViewAfterView(@NonNull View view);
    }

    private static enum Side {
        LEFT,
        RIGHT,
    }
}
