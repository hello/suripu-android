package is.hello.sense.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.animation.InteractiveAnimator;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.util.GestureInterceptingView;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Constants;

public class SlidingLayersView extends FrameLayout implements GestureInterceptingView {
    private int touchSlop;
    private int topViewOpenHeight;
    private float totalMovementHeight;
    private float startEventX, startEventY;
    private float lastEventX, lastEventY;

    private FrameLayout topViewContainer;
    private View topView;
    private float topViewY;
    private ListView listView;

    private VelocityTracker velocityTracker;
    private boolean isTrackingTouchEvents = false;
    private boolean isAnimating = false;

    private boolean isOpen = false;
    private @Nullable OnInteractionListener onInteractionListener;
    private @Nullable GestureInterceptingView gestureInterceptingChild;
    private @Nullable
    InteractiveAnimator interactiveAnimator;
    private int shadowHeight;


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
        this.topViewOpenHeight = getResources().getDimensionPixelSize(R.dimen.sliding_layers_open_height);

        this.topViewContainer = new FrameLayout(getContext());

        this.shadowHeight = getResources().getDimensionPixelSize(R.dimen.shadow_size);
        View topShadowView = new View(getContext());
        topShadowView.setBackgroundResource(R.drawable.gradient_top_layer_shadow);
        topViewContainer.addView(topShadowView, new LayoutParams(LayoutParams.MATCH_PARENT, shadowHeight, Gravity.TOP));
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

    public void openWithoutAnimation() {
        this.topView = getChildAt(1);
        this.topViewY = topView.getY();

        stopAnimations();

        if (onInteractionListener != null) {
            onInteractionListener.onUserWillPullDownTopView();
        }

        requestLayout();

        this.isOpen = true;
        this.listView = null;
    }

    public void open() {
        if (isOpen) {
            return;
        }

        this.topView = getChildAt(1);
        this.topViewY = topView.getY();

        stopAnimations();

        if (onInteractionListener != null) {
            onInteractionListener.onUserWillPullDownTopView();
        }

        if (interactiveAnimator != null) {
            interactiveAnimator.prepare();
        }

        animateOpen(Animations.DURATION_DEFAULT);
    }

    public void close() {
        if (!isOpen()) {
            return;
        }

        this.topView = getChildAt(1);
        this.topViewY = topView.getY();

        stopAnimations();

        animateClosed(Animations.DURATION_DEFAULT);
    }

    public void toggle() {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    public void setOnInteractionListener(@Nullable OnInteractionListener onInteractionListener) {
        this.onInteractionListener = onInteractionListener;
    }

    public void setGestureInterceptingChild(@Nullable GestureInterceptingView gestureInterceptingChild) {
        this.gestureInterceptingChild = gestureInterceptingChild;
    }

    public void setInteractiveAnimator(@Nullable InteractiveAnimator interactiveAnimator) {
        this.interactiveAnimator = interactiveAnimator;
    }

    @Override
    public boolean hasActiveGesture() {
        return isTrackingTouchEvents;
    }

    //endregion


    //region Layout & Drawing

    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        int normalizedIndex = index < 0 ? getChildCount() : index;

        if (normalizedIndex > 1) {
            throw new IllegalStateException("too many children for " + getClass().getSimpleName());
        }

        if (normalizedIndex == 0) {
            MarginLayoutParams marginLayoutParams;
            if (params instanceof MarginLayoutParams) {
                marginLayoutParams = (MarginLayoutParams) params;
            } else {
                marginLayoutParams = new MarginLayoutParams(params);
            }
            marginLayoutParams.bottomMargin = topViewOpenHeight - shadowHeight;
            params = marginLayoutParams;
        } else if (normalizedIndex == 1) {
            LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.TOP);
            layoutParams.setMargins(0, shadowHeight, 0, 0);
            topViewContainer.addView(child, layoutParams);
            child = topViewContainer;
            LayoutParams containerLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            containerLayoutParams.topMargin = -shadowHeight;
            params = containerLayoutParams;
        }

        super.addView(child, index, params);
    }

    @Override
    public void removeView(@NonNull View view) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeViewAt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (isOpen && getChildAt(1).getY() < topViewOpenHeight) {
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
                if (childScrollableView != null) {
                    return childScrollableView;
                }
            }
        }

        return null;
    }

    protected boolean isListViewAtTop(@Nullable ListView listView) {
        return ((topViewY > 0f) || (listView == null) ||
                (ListViews.getEstimatedScrollY(listView) == 0));
    }

    //endregion


    //region Event Handling

    private boolean shouldSnapOpen(float velocity) {
        if (isOpen) {
            return (topViewY > (getMeasuredHeight() - topViewOpenHeight * 2));
        } else {
            return (topViewY > 0f && (topViewY > topViewOpenHeight || velocity > Constants.OPEN_VELOCITY_THRESHOLD));
        }
    }

    private void stopAnimations() {
        this.isAnimating = false;
        PropertyAnimatorProxy.stop(topView);
        if (interactiveAnimator != null) {
            interactiveAnimator.cancel();
        }
    }

    private void animateOpen(long duration) {
        this.isAnimating = true;
        PropertyAnimatorProxy.animate(topView)
                .y(getMeasuredHeight() - topViewOpenHeight)
                .setDuration(duration)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        this.isAnimating = false;
                        this.isOpen = true;
                        this.listView = null;
                    }
                })
                .start();

        if (interactiveAnimator != null) {
            interactiveAnimator.finish(1f, duration, Animations.INTERPOLATOR_DEFAULT);
        }
    }

    private void animateClosed(long duration) {
        this.isAnimating = true;
        PropertyAnimatorProxy.animate(topView)
                .y(-shadowHeight)
                .setDuration(duration)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        this.isAnimating = false;
                        this.isOpen = false;
                        this.listView = null;

                        if (onInteractionListener != null) {
                            onInteractionListener.onUserDidPushUpTopView();
                        }
                    }
                })
                .start();

        if (interactiveAnimator != null) {
            interactiveAnimator.finish(0f, duration, Animations.INTERPOLATOR_DEFAULT);
        }
    }


    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (isAnimating) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                if (isTrackingTouchEvents) {
                    if (velocityTracker == null) {
                        this.velocityTracker = VelocityTracker.obtain();
                    }

                    velocityTracker.addMovement(event);

                    float y = Views.getNormalizedY(event);
                    float deltaY = y - lastEventY;
                    float newY = Math.max(0f, Math.min(totalMovementHeight, topViewY + deltaY));

                    if (interactiveAnimator != null) {
                        float amount = newY / totalMovementHeight;
                        interactiveAnimator.frame(amount);
                    }

                    topView.setY(topViewY);
                    topViewY = newY;
                }

                this.lastEventX = Views.getNormalizedX(event);
                this.lastEventY = Views.getNormalizedY(event);

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                float travelX = Math.abs(startEventX - event.getX()),
                      travelY = Math.abs(startEventY - event.getY());
                if (velocityTracker == null || isOpen && travelX < touchSlop && travelY < touchSlop) {
                    animateClosed(Animations.DURATION_DEFAULT);
                } else {
                    velocityTracker.computeCurrentVelocity(1000);
                    float velocity = Math.abs(velocityTracker.getYVelocity());
                    long duration = Animations.calculateDuration(velocity, getMeasuredHeight());
                    if (shouldSnapOpen(velocity)) {
                        animateOpen(duration);
                    } else {
                        animateClosed(duration);
                    }

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
        if (isAnimating) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.lastEventX = Views.getNormalizedX(event);
                this.startEventX = lastEventX;
                this.lastEventY = Views.getNormalizedY(event);
                this.startEventY = lastEventY;
                this.listView = findFirstViewIn(ListView.class, this);

                this.topView = getChildAt(1);
                this.topViewY = topView.getY();

                this.totalMovementHeight = getMeasuredHeight() - topViewOpenHeight;

                stopAnimations();

                if (isOpen && lastEventY >= topViewY) {
                    this.isTrackingTouchEvents = true;
                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (gestureInterceptingChild != null && gestureInterceptingChild.hasActiveGesture()) {
                    return false;
                }

                float x = Views.getNormalizedX(event), y = Views.getNormalizedY(event);
                float deltaX = x - lastEventX;
                float deltaY = y - lastEventY;
                if (Math.abs(deltaY) >= touchSlop && Math.abs(deltaY) > Math.abs(deltaX) &&
                        (!isOpen && deltaY > 0.0) && isListViewAtTop(listView)) {
                    this.isTrackingTouchEvents = true;

                    if (onInteractionListener != null) {
                        onInteractionListener.onUserWillPullDownTopView();
                    }

                    if (interactiveAnimator != null) {
                        interactiveAnimator.prepare();
                    }

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
