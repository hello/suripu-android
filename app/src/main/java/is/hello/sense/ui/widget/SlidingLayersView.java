package is.hello.sense.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.ui.widget.util.InteractiveAnimator;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Constants;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SlidingLayersView extends FrameLayout {
    private int touchSlop;
    private int topViewOpenHeight;
    private float totalMovementHeight;
    private float startEventX, startEventY;
    private float lastEventX, lastEventY;

    private FrameLayout topViewContainer;
    private View topView;
    private float topViewY;
    private RecyclerView recyclerView;

    private @Nullable VelocityTracker velocityTracker;
    private boolean trackingTouchEvents = false;
    private boolean animating = false;

    private boolean isOpen = false;
    private @Nullable Listener listener;
    private @Nullable InteractiveAnimator interactiveAnimator;
    private @Nullable AnimatorContext animatorContext;
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
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

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

            post(() -> {
                if (isOpen && listener != null) {
                    listener.onTopViewWillSlideDown();
                }
            });
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

        if (listener != null) {
            listener.onTopViewWillSlideDown();
        }

        requestLayout();

        this.isOpen = true;
        this.recyclerView = null;
    }

    public void open() {
        if (isOpen) {
            return;
        }

        this.topView = getChildAt(1);
        this.topViewY = topView.getY();

        stopAnimations();

        if (listener != null) {
            listener.onTopViewWillSlideDown();
        }

        if (interactiveAnimator != null) {
            interactiveAnimator.prepare();
        }

        animateOpen(Anime.DURATION_NORMAL);
    }

    public void close() {
        if (!isOpen()) {
            return;
        }

        this.topView = getChildAt(1);
        this.topViewY = topView.getY();

        stopAnimations();

        animateClosed(Anime.DURATION_NORMAL);
    }

    public void toggle() {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setInteractiveAnimator(@Nullable InteractiveAnimator interactiveAnimator) {
        this.interactiveAnimator = interactiveAnimator;
    }

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public boolean isInMotion() {
        return (animating || trackingTouchEvents);
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

        int intendedY = MeasureSpec.getSize(heightMeasureSpec) - topViewOpenHeight;
        if (!animating && isOpen && getChildAt(1).getY() != intendedY) {
            getChildAt(1).setY(intendedY);
        }
    }

    //endregion


    //region Scrollable children

    protected @Nullable <T extends View> T findFirstViewIn(@NonNull Class<T> viewClass, @NonNull ViewGroup view, @NonNull MotionEvent event) {
        for (int i = 0, c = view.getChildCount(); i < c; i++) {
            View child = view.getChildAt(i);
            if (!Views.isMotionEventInside(child, event)) {
                continue;
            }

            if (viewClass.isAssignableFrom(child.getClass())) {
                // noinspection unchecked
                return (T) child;
            } else if (child instanceof ViewGroup) {
                T childScrollableView = findFirstViewIn(viewClass, (ViewGroup) child, event);
                if (childScrollableView != null) {
                    return childScrollableView;
                }
            }
        }

        return null;
    }

    protected boolean isRecyclerViewAtTop(@Nullable RecyclerView recyclerView) {
        if ((topViewY > 0f) || (recyclerView == null)) {
            return true;
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        return (layoutManager.findFirstCompletelyVisibleItemPosition() == 0);
    }

    //endregion


    //region Event Handling

    private boolean shouldSnapOpen(float velocity) {
        if (isOpen) {
            return (velocity > Constants.OPEN_VELOCITY_THRESHOLD ||
                    topViewY > (getMeasuredHeight() - topViewOpenHeight * 2));
        } else {
            return (topViewY > 0f && (topViewY > topViewOpenHeight ||
                    velocity > Constants.OPEN_VELOCITY_THRESHOLD));
        }
    }

    private void stopAnimations() {
        this.animating = false;
        Anime.cancelAll(topView);
        if (interactiveAnimator != null) {
            interactiveAnimator.cancel();
        }
    }

    private void animateOpen(long duration) {
        this.animating = true;
        animatorFor(topView, animatorContext)
                .y(getMeasuredHeight() - topViewOpenHeight)
                .withDuration(duration)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        this.animating = false;
                        this.isOpen = true;
                        this.recyclerView = null;
                    }
                })
                .start();

        if (interactiveAnimator != null) {
            interactiveAnimator.finish(1f, duration, Anime.INTERPOLATOR_DEFAULT, animatorContext);
        }
    }

    private void animateClosed(long duration) {
        this.animating = true;
        animatorFor(topView, animatorContext)
                .y(-shadowHeight)
                .withDuration(duration)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        this.animating = false;
                        this.isOpen = false;
                        this.recyclerView = null;

                        if (listener != null) {
                            listener.onTopViewDidSlideUp();
                        }
                    }
                })
                .start();

        if (interactiveAnimator != null) {
            interactiveAnimator.finish(0f, duration, Anime.INTERPOLATOR_DEFAULT, animatorContext);
        }
    }


    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (animating) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                if (trackingTouchEvents) {
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
                    animateClosed(Anime.DURATION_NORMAL);
                } else {
                    velocityTracker.computeCurrentVelocity(1000);
                    float velocity = velocityTracker.getYVelocity();
                    long duration = Anime.calculateDuration(velocity, getMeasuredHeight());
                    if (shouldSnapOpen(velocity)) {
                        animateOpen(duration);
                    } else {
                        animateClosed(duration);
                    }

                    velocityTracker.recycle();
                }

                if (trackingTouchEvents && animatorContext != null) {
                    animatorContext.endAnimation();
                }

                this.velocityTracker = null;
                this.trackingTouchEvents = false;

                return true;
            }
        }

        return trackingTouchEvents;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        if (animating) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.lastEventX = Views.getNormalizedX(event);
                this.startEventX = lastEventX;
                this.lastEventY = Views.getNormalizedY(event);
                this.startEventY = lastEventY;
                this.recyclerView = findFirstViewIn(RecyclerView.class, this, event);

                this.topView = getChildAt(1);
                this.topViewY = topView.getY();

                this.totalMovementHeight = getMeasuredHeight() - topViewOpenHeight;

                stopAnimations();

                if (isOpen && lastEventY >= topViewY) {
                    if (animatorContext != null) {
                        animatorContext.beginAnimation();
                    }

                    this.trackingTouchEvents = true;
                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                float x = Views.getNormalizedX(event), y = Views.getNormalizedY(event);
                float deltaX = x - lastEventX;
                float deltaY = y - lastEventY;
                if (Math.abs(deltaY) >= touchSlop && Math.abs(deltaY) > Math.abs(deltaX) &&
                        (!isOpen && deltaY > 0.0) && isRecyclerViewAtTop(recyclerView)) {
                    this.trackingTouchEvents = true;

                    if (animatorContext != null) {
                        animatorContext.beginAnimation();
                    }

                    if (listener != null) {
                        listener.onTopViewWillSlideDown();
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


    public interface Listener {
        void onTopViewWillSlideDown();
        void onTopViewDidSlideUp();
    }
}
