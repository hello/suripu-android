package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
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
    private static final String ANIMATOR_NAME = SlidingLayersView.class.getSimpleName() + "#onTouchEvent";

    private final int touchSlop;
    private final int baseTopOpenHeight;
    private final int topDividerHeight;
    private float totalMovementHeight;
    private float startEventX, startEventY;
    private float lastEventX, lastEventY;

    private View topView;
    private float topTranslationY;
    private RecyclerView recyclerView;

    private @Nullable VelocityTracker velocityTracker;
    private boolean trackingTouchEvents = false;
    private boolean animating = false;

    private boolean open = false;
    private @Nullable Listener listener;
    private @Nullable InteractiveAnimator interactiveAnimator;
    private @Nullable AnimatorContext animatorContext;
    private float topExtraTranslationAmount = 0.0f;


    //region Lifecycle

    public SlidingLayersView(@NonNull Context context) {
        this(context, null);
    }

    public SlidingLayersView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingLayersView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setWillNotDraw(false);

        final Resources resources = getResources();

        // ListView eats some vertical motion events, so our touch slop has
        // to be lower than standard in order for the swipe gesture to work.
        this.touchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop() / 2;
        this.baseTopOpenHeight = resources.getDimensionPixelSize(R.dimen.sliding_layers_open_height);
        this.topDividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            this.open = savedState.getBoolean("open");
            state = savedState.getParcelable("savedState");

            requestLayout();

            post(() -> {
                if (open && listener != null) {
                    listener.onTopViewWillSlideDown();
                }
            });
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle savedState = new Bundle();
        savedState.putBoolean("open", isOpen());
        savedState.putParcelable("savedState", super.onSaveInstanceState());
        return savedState;
    }

    //endregion


    //region Properties

    public boolean isOpen() {
        return open;
    }

    public void openWithoutAnimation() {
        this.topView = getChildAt(1);
        this.topTranslationY = topView.getTranslationY();

        stopAnimations();

        if (listener != null) {
            listener.onTopViewWillSlideDown();
        }

        requestLayout();

        this.open = true;
        this.recyclerView = null;
    }

    public void open() {
        if (open) {
            return;
        }

        this.topView = getChildAt(1);
        this.topTranslationY = topView.getTranslationY();

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
        this.topTranslationY = topView.getTranslationY();

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

    public void setTopExtraTranslationAmount(float topExtraTranslationAmount) {
        this.topExtraTranslationAmount = topExtraTranslationAmount;

        if (open && !animating) {
            final int newTranslationY = getMeasuredHeight() - getTopOpenHeight();
            topView.setTranslationY(newTranslationY);
        }

        this.totalMovementHeight = getMeasuredHeight() - getTopOpenHeight();
    }

    //endregion


    //region Layout & Drawing

    private int getTopOpenHeight() {
        return Math.round(baseTopOpenHeight * (1f - topExtraTranslationAmount));
    }

    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        final int normalizedIndex = index < 0 ? getChildCount() : index;

        if (normalizedIndex > 1) {
            throw new IllegalStateException("too many children for " + getClass().getSimpleName());
        }

        if (normalizedIndex == 1) {
            final LayoutParams updatedParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                                LayoutParams.MATCH_PARENT);
            updatedParams.topMargin = -topDividerHeight;
            params = updatedParams;
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

        final int intendedY = MeasureSpec.getSize(heightMeasureSpec) - getTopOpenHeight();
        if (!animating && open && getChildAt(1).getTranslationY() != intendedY) {
            getChildAt(1).setTranslationY(intendedY);
        }
    }

    //endregion


    //region Scrollable children

    protected @Nullable <T extends View> T findFirstViewIn(@NonNull Class<T> viewClass,
                                                           @NonNull ViewGroup view,
                                                           @NonNull MotionEvent event) {
        for (int i = 0, c = view.getChildCount(); i < c; i++) {
            final View child = view.getChildAt(i);
            if (!Views.isMotionEventInside(child, event)) {
                continue;
            }

            if (viewClass.isAssignableFrom(child.getClass())) {
                // noinspection unchecked
                return (T) child;
            } else if (child instanceof ViewGroup) {
                final T childScrollableView = findFirstViewIn(viewClass, (ViewGroup) child, event);
                if (childScrollableView != null) {
                    return childScrollableView;
                }
            }
        }

        return null;
    }

    protected boolean isRecyclerViewAtTop(@Nullable RecyclerView recyclerView) {
        if ((topTranslationY > 0f) || (recyclerView == null)) {
            return true;
        }

        final LinearLayoutManager layoutManager =
                (LinearLayoutManager) recyclerView.getLayoutManager();
        return (layoutManager.findFirstCompletelyVisibleItemPosition() == 0);
    }

    //endregion


    //region Event Handling

    private boolean shouldSnapOpen(float velocity) {
        if (open) {
            return (velocity > Constants.OPEN_VELOCITY_THRESHOLD ||
                    topTranslationY > (getMeasuredHeight() - baseTopOpenHeight * 2));
        } else {
            return (topTranslationY > 0f && (topTranslationY > baseTopOpenHeight ||
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
                .translationY(getMeasuredHeight() - getTopOpenHeight())
                .withDuration(duration)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        this.animating = false;
                        this.open = true;
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
                .translationY(0f)
                .withDuration(duration)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        this.animating = false;
                        this.open = false;
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

                    final float y = Views.getNormalizedY(event);
                    final float deltaY = y - lastEventY;
                    final float newY = Math.max(0f, Math.min(totalMovementHeight, topTranslationY + deltaY));

                    if (interactiveAnimator != null) {
                        float amount = newY / totalMovementHeight;
                        interactiveAnimator.frame(amount);
                    }

                    topView.setTranslationY(topTranslationY);
                    topTranslationY = newY;
                }

                this.lastEventX = Views.getNormalizedX(event);
                this.lastEventY = Views.getNormalizedY(event);

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                final float travelX = Math.abs(startEventX - event.getX()),
                            travelY = Math.abs(startEventY - event.getY());
                if (velocityTracker == null || open && travelX < touchSlop && travelY < touchSlop) {
                    animateClosed(Anime.DURATION_NORMAL);
                } else {
                    velocityTracker.computeCurrentVelocity(1000);
                    final float velocity = velocityTracker.getYVelocity();
                    final long duration = Anime.calculateDuration(velocity, getMeasuredHeight());
                    if (shouldSnapOpen(velocity)) {
                        animateOpen(duration);
                    } else {
                        animateClosed(duration);
                    }

                    velocityTracker.recycle();
                }

                if (trackingTouchEvents && animatorContext != null) {
                    animatorContext.endAnimation(ANIMATOR_NAME);
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
                this.topTranslationY = topView.getTranslationY();

                this.totalMovementHeight = getMeasuredHeight() - getTopOpenHeight();

                stopAnimations();

                if (open && lastEventY >= topTranslationY) {
                    if (animatorContext != null) {
                        animatorContext.beginAnimation(ANIMATOR_NAME);
                    }

                    this.trackingTouchEvents = true;
                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final float x = Views.getNormalizedX(event), y = Views.getNormalizedY(event);
                final float deltaX = x - lastEventX;
                final float deltaY = y - lastEventY;
                if (Math.abs(deltaY) >= touchSlop && Math.abs(deltaY) > Math.abs(deltaX) &&
                        (!open && deltaY > 0.0) && isRecyclerViewAtTop(recyclerView)) {
                    this.trackingTouchEvents = true;

                    if (animatorContext != null) {
                        animatorContext.beginAnimation(ANIMATOR_NAME);
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
