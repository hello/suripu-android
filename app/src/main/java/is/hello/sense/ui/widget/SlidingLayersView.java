package is.hello.sense.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;

import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.util.Constants;

public class SlidingLayersView extends FrameLayout implements GestureInterceptingView {
    private int touchSlop;
    private int topViewOpenHeight;
    private float lastEventX, lastEventY;

    private View topView;
    private float topViewY;
    private ListView listView;

    private VelocityTracker velocityTracker;
    private boolean isTrackingTouchEvents = false;

    private boolean isOpen = false;

    private OnInteractionListener onInteractionListener;

    private GestureInterceptingView gestureInterceptingChild;


    //region Lifecycle

    @SuppressWarnings("UnusedDeclaration")
    public SlidingLayersView(Context context) {
        super(context);
        initialize();
    }

    @SuppressWarnings("UnusedDeclaration")
    public SlidingLayersView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    @SuppressWarnings("UnusedDeclaration")
    public SlidingLayersView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        // ListView eats some vertical motion events, so our touch slop has
        // to be lower than standard in order for the swipe gesture to work.
        this.touchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop() / 2;
        this.topViewOpenHeight = (int) (getResources().getDisplayMetrics().density * 60f);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle savedState = (Bundle) state;
            this.isOpen = savedState.getBoolean("isOpen");
            state = savedState.getParcelable("savedState");

            requestLayout();
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle savedState = new Bundle();
        savedState.putBoolean("isOpen", isOpen());
        savedState.putParcelable("savedState", super.onSaveInstanceState());
        return savedState;
    }

    //endregion


    //region Properties

    public boolean isOpen() {
        return isOpen;
    }

    public void open() {
        if (isOpen)
            return;

        animateOpen(Animation.DURATION_DEFAULT);
    }

    public void close() {
        if (!isOpen())
            return;

        animateClosed(Animation.DURATION_DEFAULT);
    }

    public OnInteractionListener getOnInteractionListener() {
        return onInteractionListener;
    }

    public void setOnInteractionListener(OnInteractionListener onInteractionListener) {
        this.onInteractionListener = onInteractionListener;
    }

    public GestureInterceptingView getGestureInterceptingChild() {
        return gestureInterceptingChild;
    }

    public void setGestureInterceptingChild(GestureInterceptingView gestureInterceptingChild) {
        this.gestureInterceptingChild = gestureInterceptingChild;
    }

    @Override
    public boolean hasActiveGesture() {
        return isTrackingTouchEvents;
    }

    //endregion


    //region Layout

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (isOpen && getChildAt(1).getY() == 0f) {
            getChildAt(1).setY(getMeasuredHeight() - topViewOpenHeight);
        }
    }

    //endregion


    //region List View Hack

    // Besides eating events, ListView being scrollable mucks up our gesture
    // too. So we look for any child list views the first time the user puts
    // their finger down and then make sure said list view is scrolled to the
    // top before we let the user pull down the top view.

    protected @Nullable <T extends View> T findFirstViewIn(Class<T> viewClass, ViewGroup view) {
        for (int i = 0, c = view.getChildCount(); i < c; i++) {
            View child = view.getChildAt(i);
            if (viewClass.isAssignableFrom(child.getClass())) {
                // noinspection unchecked
                return (T) child;
            } else if (child instanceof ViewGroup) {
                T childScrollableView = findFirstViewIn(viewClass, (ViewGroup) child);
                if (childScrollableView != null)
                    return childScrollableView;
            }
        }

        return null;
    }

    protected int calculateListViewOffset(@Nullable ListView listView) {
        if (listView == null)
            return 0;
        
        View rowView = listView.getChildAt(0);
        return -rowView.getTop() + listView.getFirstVisiblePosition() * rowView.getHeight();
    }

    //endregion


    //region Event Handling

    private boolean shouldSnapOpen(float velocity) {
        if (isOpen)
            return (topViewY > (getMeasuredHeight() - topViewOpenHeight * 2));
        else
            return (topViewY > 0f && (topViewY > topViewOpenHeight || velocity > Constants.OPEN_VELOCITY_THRESHOLD));
    }

    private void animateOpen(long duration) {
        PropertyAnimatorProxy.animate(topView)
                .y(getMeasuredHeight() - topViewOpenHeight)
                .setDuration(duration)
                .setApplyChangesToView(true)
                .setOnAnimationCompleted(finished -> {
                    if (finished) {
                        this.isOpen = true;
                        this.listView = null;
                    }
                })
                .start();
    }

    private void animateClosed(long duration) {
        PropertyAnimatorProxy.animate(topView)
                .y(0f)
                .setDuration(duration)
                .setApplyChangesToView(true)
                .setOnAnimationCompleted(finished -> {
                    if (finished) {
                        this.isOpen = false;
                        this.listView = null;

                        if (onInteractionListener != null)
                            onInteractionListener.onUserDidPushUpTopView();
                    }
                })
                .start();
    }


    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                if (isTrackingTouchEvents) {
                    if (velocityTracker == null)
                        this.velocityTracker = VelocityTracker.obtain();

                    velocityTracker.addMovement(event);

                    float y = event.getY();
                    float deltaY = y - lastEventY;
                    float newY = Math.max(0f, topViewY + deltaY);

                    topView.setY(topViewY);
                    topViewY = newY;
                }

                this.lastEventX = event.getX();
                this.lastEventY = event.getY();

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (velocityTracker == null) {
                    animateClosed(Animation.DURATION_DEFAULT);
                } else {
                    velocityTracker.computeCurrentVelocity(1000);
                    float velocity = Math.abs(velocityTracker.getYVelocity());
                    long duration = Animation.calculateDuration(velocity, getMeasuredHeight());
                    if (shouldSnapOpen(velocity))
                        animateOpen(duration);
                    else
                        animateClosed(duration);

                    velocityTracker.recycle();
                }

                this.velocityTracker = null;
                this.isTrackingTouchEvents = false;

                return true;
            }
        }

        return isTrackingTouchEvents;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.lastEventX = event.getX();
                this.lastEventY = event.getY();
                this.listView = findFirstViewIn(ListView.class, this);

                this.topView = getChildAt(1);
                this.topViewY = topView.getY();

                PropertyAnimatorProxy.stopAnimating(topView);

                if (isOpen && lastEventY >= getMeasuredHeight() - topViewOpenHeight) {
                    isTrackingTouchEvents = true;
                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (gestureInterceptingChild != null && gestureInterceptingChild.hasActiveGesture())
                    return false;

                float x = event.getX(), y = event.getY();
                float deltaX = x - lastEventX;
                float deltaY = y - lastEventY;
                if (Math.abs(deltaY) >= touchSlop && Math.abs(deltaY) > Math.abs(deltaX) &&
                        (!isOpen && deltaY > 0.0) && calculateListViewOffset(listView) == 0) {
                    this.isTrackingTouchEvents = true;

                    if (onInteractionListener != null)
                        onInteractionListener.onUserWillPullDownTopView();

                    return true;
                }
            }
        }

        return false;
    }

    //endregion


    public interface OnInteractionListener {
        void onUserWillPullDownTopView();
        void onUserDidPushUpTopView();
    }
}
