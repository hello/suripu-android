package is.hello.sense.ui.widget;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.util.GestureInterceptingView;
import is.hello.sense.util.Constants;
import is.hello.sense.util.ResumeScheduler;

public final class FragmentPageView<TFragment extends Fragment> extends FrameLayout implements GestureInterceptingView {
    //region Property Fields

    private Adapter<TFragment> adapter;
    private OnTransitionObserver<TFragment> onTransitionObserver;
    private FragmentManager fragmentManager;
    private @Nullable ResumeScheduler.Coordinator resumeCoordinator;
    private Animations.Properties animationProperties = Animations.Properties.create(p -> {
        p.interpolator = new DecelerateInterpolator();
        return null;
    });

    //endregion


    //region Views

    /* Do not access these fields directly, use the on- and offScreenView methods */
    private FrameLayout view1;
    private FrameLayout view2;
    private boolean viewsSwapped = false;

    //endregion


    //region Event Handling

    private int touchSlop;
    private VelocityTracker velocityTracker;
    private EdgeEffect leftEdgeEffect;
    private EdgeEffect rightEdgeEffect;

    private int viewWidth;
    private float lastEventX, lastEventY;
    private float viewX;
    private Position currentPosition;
    private boolean hasBeforeView = false, hasAfterView = false;
    private boolean isTrackingTouchEvents = false;

    private boolean isAnimating = false;

    //endregion


    //region Creation

    @SuppressWarnings("UnusedDeclaration")
    public FragmentPageView(Context context) {
        super(context);
        initialize();
    }

    @SuppressWarnings("UnusedDeclaration")
    public FragmentPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    @SuppressWarnings("UnusedDeclaration")
    public FragmentPageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        setWillNotDraw(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);

        this.touchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        this.leftEdgeEffect = new EdgeEffect(getContext());
        this.rightEdgeEffect = new EdgeEffect(getContext());

        this.view1 = new FrameLayout(getContext());
        view1.setId(R.id.fragment_page_view_on_screen);
        addView(view1);

        this.view2 = new FrameLayout(getContext());
        view2.setId(R.id.fragment_page_view_off_screen);
        view2.setVisibility(INVISIBLE);
        addView(view2);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state != null && state instanceof Bundle) {
            boolean viewsSwapped = ((Bundle) state).getBoolean("viewsSwapped");

            if (viewsSwapped) {
                this.viewsSwapped = true;
                getOffScreenView().setVisibility(INVISIBLE);
                getOnScreenView().setVisibility(VISIBLE);
            }

            state = ((Bundle) state).getParcelable("instanceState");
        }

        super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle savedState = new Bundle();

        savedState.putParcelable("instanceState", super.onSaveInstanceState());
        savedState.putBoolean("viewsSwapped", viewsSwapped);

        return savedState;
    }

    //endregion


    //region Properties

    protected void assertFragmentManager() {
        if (fragmentManager == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " requires a fragment manager to operate.");
        }
    }

    public Adapter<TFragment> getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter<TFragment> adapter) {
        this.adapter = adapter;
    }

    public OnTransitionObserver<TFragment> getOnTransitionObserver() {
        return onTransitionObserver;
    }

    public void setOnTransitionObserver(OnTransitionObserver<TFragment> onTransitionObserver) {
        this.onTransitionObserver = onTransitionObserver;
    }

    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public @Nullable ResumeScheduler.Coordinator getResumeCoordinator() {
        return resumeCoordinator;
    }

    public void setResumeCoordinator(@Nullable ResumeScheduler.Coordinator resumeCoordinator) {
        this.resumeCoordinator = resumeCoordinator;
    }

    public TFragment getCurrentFragment() {
        // noinspection unchecked
        return (TFragment) getFragmentManager().findFragmentById(getOnScreenView().getId());
    }

    public void setCurrentFragment(TFragment newFragment) {
        assertFragmentManager();

        TFragment currentFragment = getCurrentFragment();
        if (newFragment != null) {
            if (getOnTransitionObserver() != null) {
                getOnTransitionObserver().onWillTransitionToFragment(this, newFragment);
                post(() -> getOnTransitionObserver().onDidTransitionToFragment(this, newFragment));
            }

            if (currentFragment != null) {
                getFragmentManager().beginTransaction()
                        .replace(getOnScreenView().getId(), newFragment)
                        .commit();
            } else {
                getFragmentManager().beginTransaction()
                        .add(getOnScreenView().getId(), newFragment)
                        .commit();
            }
        } else if (currentFragment != null) {
            getFragmentManager().beginTransaction()
                    .remove(currentFragment)
                    .commit();
        }

    }

    @Override
    public boolean hasActiveGesture() {
        return isTrackingTouchEvents;
    }

    //endregion


    //region Subviews

    private FrameLayout getOnScreenView() {
        if (viewsSwapped)
            return view2;
        else
            return view1;
    }

    private FrameLayout getOffScreenView() {
        if (viewsSwapped)
            return view1;
        else
            return view2;
    }

    //endregion


    //region Drawing

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        if (getOverScrollMode() == OVER_SCROLL_NEVER) {
            leftEdgeEffect.finish();
            rightEdgeEffect.finish();
        } else {
            boolean needsInvalidate = false;
            if (!leftEdgeEffect.isFinished()) {
                canvas.save();
                {
                    canvas.rotate(270f);
                    canvas.translate(-getHeight(), 0);
                    leftEdgeEffect.setSize(getHeight(), getWidth());
                    needsInvalidate = leftEdgeEffect.draw(canvas);
                }
                canvas.restore();
            }

            if (!rightEdgeEffect.isFinished()) {
                canvas.save();
                {
                    canvas.rotate(90f);
                    canvas.translate(0, -getWidth());
                    rightEdgeEffect.setSize(getHeight(), getWidth());
                    needsInvalidate |= rightEdgeEffect.draw(canvas);
                }
                canvas.restore();
            }

            if (needsInvalidate)
                invalidate();
        }
    }


    //endregion


    //region Events

    private boolean isPositionValid(Position position) {
        return (position == Position.BEFORE && hasBeforeView ||
                position == Position.AFTER && hasAfterView);
    }

    private boolean shouldCompleteTransition(float rawViewX, float rawVelocity) {
        if (rawViewX == 0f)
            return false;

        if (rawViewX < 0f) {
            return (Math.abs(rawViewX) > viewWidth / 4 || rawVelocity < -Constants.OPEN_VELOCITY_THRESHOLD);
        } else {
            return (Math.abs(rawViewX) > viewWidth / 4 || rawVelocity > Constants.OPEN_VELOCITY_THRESHOLD);
        }
    }

    private TFragment getOffScreenFragment() {
        //noinspection unchecked
        return (TFragment) getFragmentManager().findFragmentById(getOffScreenView().getId());
    }

    private void removeOffScreenFragment() {
        TFragment offScreen = getOffScreenFragment();
        if (offScreen != null) {
            if (getResumeCoordinator() != null) {
                getResumeCoordinator().postOnResume(() -> {
                    getFragmentManager()
                            .beginTransaction()
                            .remove(offScreen)
                            .commit();
                });
            } else {
                getFragmentManager()
                        .beginTransaction()
                        .remove(offScreen)
                        .commit();
            }
        }

        getOffScreenView().setVisibility(INVISIBLE);
    }

    private void addOffScreenFragment(Position position) {
        TFragment newFragment = null;
        switch (position) {
            case BEFORE:
                newFragment = adapter.getFragmentBeforeFragment(getCurrentFragment());
                break;

            case AFTER:
                newFragment = adapter.getFragmentAfterFragment(getCurrentFragment());
                break;
        }

        getFragmentManager().beginTransaction()
                .add(getOffScreenView().getId(), newFragment)
                .commit();

        getOffScreenView().setVisibility(VISIBLE);
    }

    private void exchangeOnAndOffScreen() {
        viewsSwapped = !viewsSwapped;

        removeOffScreenFragment();
        getOnScreenView().setX(0f);
    }

    private void completeTransition(Position position, long duration) {
        PropertyAnimatorProxy onScreenViewAnimator = animationProperties.toPropertyAnimator(getOnScreenView()).setDuration(duration);
        PropertyAnimatorProxy offScreenViewAnimator = animationProperties.toPropertyAnimator(getOffScreenView()).setDuration(duration);

        offScreenViewAnimator.x(0f);
        onScreenViewAnimator.x(position == Position.BEFORE ? viewWidth : -viewWidth);

        onScreenViewAnimator.addOnAnimationCompleted(finished -> {
            if (!finished)
                return;

            this.currentPosition = null;
            velocityTracker.recycle();
            this.velocityTracker = null;

            exchangeOnAndOffScreen();

            if (getOnTransitionObserver() != null) {
                getOnTransitionObserver().onDidTransitionToFragment(this, getCurrentFragment());
            }

            this.isAnimating = false;
        });

        if (getOnTransitionObserver() != null) {
            getOnTransitionObserver().onWillTransitionToFragment(this, getOffScreenFragment());
        }

        onScreenViewAnimator.start();
        offScreenViewAnimator.start();

        this.isAnimating = true;
    }

    private void snapBack(Position position, long duration) {
        PropertyAnimatorProxy onScreenViewAnimator = animationProperties.toPropertyAnimator(getOnScreenView()).setDuration(duration);
        PropertyAnimatorProxy offScreenViewAnimator = animationProperties.toPropertyAnimator(getOffScreenView()).setDuration(duration);

        offScreenViewAnimator.x(position == Position.BEFORE ? -viewWidth : viewWidth);
        onScreenViewAnimator.x(0f);
        onScreenViewAnimator.addOnAnimationCompleted(finished -> {
            if (!finished)
                return;

            this.currentPosition = null;
            velocityTracker.recycle();
            this.velocityTracker = null;

            removeOffScreenFragment();
            getOnScreenView().setX(0f);
            getOffScreenView().setVisibility(INVISIBLE);

            if (getOnTransitionObserver() != null) {
                getOnTransitionObserver().onDidSnapBackToFragment(this, getCurrentFragment());
            }

            this.isAnimating = false;
        });

        onScreenViewAnimator.start();
        offScreenViewAnimator.start();

        this.isAnimating = true;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                if (isTrackingTouchEvents) {
                    float x = event.getRawX(),
                          y = event.getRawY();
                    float deltaX = x - lastEventX;

                    velocityTracker.addMovement(event);

                    if (Math.abs(y - lastEventY) < touchSlop) {
                        float newX = viewX + deltaX;
                        Position position = newX > 0.0 ? Position.BEFORE : Position.AFTER;
                        if (position != currentPosition) {
                            removeOffScreenFragment();

                            if (isPositionValid(position)) {
                                addOffScreenFragment(position);
                            }

                            this.currentPosition = position;
                        }

                        if (isPositionValid(position)) {
                            getOffScreenView().setX(position == Position.BEFORE ? newX - viewWidth : newX + viewWidth);
                            getOnScreenView().setX(newX);

                            this.viewX = newX;
                        } else {
                            this.viewX = 0;
                            getOnScreenView().setX(0);

                            if (position == Position.BEFORE) {
                                leftEdgeEffect.onPull(-deltaX / viewWidth);
                            } else {
                                rightEdgeEffect.onPull(deltaX / viewWidth);
                            }
                            invalidate();
                        }
                    }

                    this.lastEventX = x;
                    this.lastEventY = y;

                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (isTrackingTouchEvents) {
                    velocityTracker.computeCurrentVelocity(1000);
                    float rawVelocity = velocityTracker.getXVelocity();
                    float velocity = Math.abs(rawVelocity);
                    long duration = Animations.calculateDuration(velocity, getMeasuredWidth());

                    if (shouldCompleteTransition(viewX, rawVelocity))
                        completeTransition(currentPosition, duration);
                    else
                        snapBack(currentPosition, duration);

                    boolean shouldInvalidate = false;
                    if (!leftEdgeEffect.isFinished()) {
                        leftEdgeEffect.onRelease();

                        shouldInvalidate = true;
                    }

                    if (!rightEdgeEffect.isFinished()) {
                        rightEdgeEffect.onRelease();

                        shouldInvalidate = true;
                    }

                    if (shouldInvalidate)
                        invalidate();

                    this.isTrackingTouchEvents = false;

                    return true;
                }

                break;
            }
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isAnimating) {
                    PropertyAnimatorProxy.stop(getOnScreenView(), getOffScreenView());
                    this.isTrackingTouchEvents = true;
                } else {
                    this.lastEventX = event.getRawX();
                    this.lastEventY = event.getRawY();
                    this.viewX = getOnScreenView().getX();
                    this.viewWidth = getOnScreenView().getMeasuredWidth();

                    this.hasBeforeView = adapter.hasFragmentBeforeFragment(getCurrentFragment());
                    this.hasAfterView = adapter.hasFragmentAfterFragment(getCurrentFragment());
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (isAnimating && isTrackingTouchEvents) {
                    this.isAnimating = false;

                    return true;
                }

                float x = event.getRawX(), y = event.getRawY();
                float deltaX = x - lastEventX;
                float deltaY = y - lastEventY;
                if (!isTrackingTouchEvents && Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY)) {
                    this.velocityTracker = VelocityTracker.obtain();
                    this.isTrackingTouchEvents = true;

                    return true;
                }

                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                this.isTrackingTouchEvents = false;
                break;
        }

        return false;
    }

    //endregion


    public interface Adapter<TFragment extends Fragment> {
        boolean hasFragmentBeforeFragment(@NonNull TFragment fragment);
        TFragment getFragmentBeforeFragment(@NonNull TFragment fragment);

        boolean hasFragmentAfterFragment(@NonNull TFragment fragment);
        TFragment getFragmentAfterFragment(@NonNull TFragment fragment);
    }

    public interface OnTransitionObserver<TFragment extends Fragment> {
        void onWillTransitionToFragment(@NonNull FragmentPageView<TFragment> view, @NonNull TFragment fragment);
        void onDidTransitionToFragment(@NonNull FragmentPageView<TFragment> view, @NonNull TFragment fragment);
        void onDidSnapBackToFragment(@NonNull FragmentPageView<TFragment> view, @NonNull TFragment fragment);
    }

    private static enum Position {
        BEFORE,
        AFTER,
    }
}
