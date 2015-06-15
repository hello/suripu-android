package is.hello.sense.ui.widget;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;
import is.hello.sense.ui.widget.util.GestureInterceptingView;
import is.hello.sense.util.Constants;
import is.hello.sense.util.StateSafeExecutor;

public final class FragmentPageView<TFragment extends Fragment> extends FrameLayout implements GestureInterceptingView {
    //region Property Fields

    private Adapter<TFragment> adapter;
    private OnTransitionObserver<TFragment> onTransitionObserver;
    private FragmentManager fragmentManager;
    private @Nullable StateSafeExecutor stateSafeExecutor;
    private final AnimatorConfig animationConfig = new AnimatorConfig(new DecelerateInterpolator());
    private @Nullable AnimatorContext animatorContext;
    private @Nullable Decor decor;

    //endregion


    //region Views

    /* Do not access these fields directly, use the on- and offScreenView methods */
    private FrameLayout view1;
    private FrameLayout view2;
    private boolean viewsSwapped = false;

    //endregion


    //region Event Handling

    private int touchSlop;
    private @Nullable VelocityTracker velocityTracker;
    private EdgeEffect leftEdgeEffect;
    private EdgeEffect rightEdgeEffect;

    private int viewWidth;
    private float lastEventX, lastEventY;
    private float viewX;
    private Position currentPosition;
    private boolean hasBeforeView = false, hasAfterView = false;
    private boolean trackingTouchEvents = false;

    private @NonNull RunningAnimation runningAnimation = RunningAnimation.NONE;

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
        setFocusable(true);

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

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
        if (ViewCompat.getImportantForAccessibility(this) == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
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

    public @Nullable StateSafeExecutor getStateSafeExecutor() {
        return stateSafeExecutor;
    }

    public void setStateSafeExecutor(@Nullable StateSafeExecutor stateSafeExecutor) {
        this.stateSafeExecutor = stateSafeExecutor;
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
                getOnTransitionObserver().onWillTransitionToFragment(newFragment, false);
                post(() -> getOnTransitionObserver().onDidTransitionToFragment(newFragment, false));
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

            if (decor != null) {
                CharSequence title = adapter.getFragmentTitle(newFragment);
                decor.onSetOnScreenTitle(title);
            }
        } else if (currentFragment != null) {
            getFragmentManager().beginTransaction()
                    .remove(currentFragment)
                    .commit();
        }
    }

    public void animateToFragment(TFragment newFragment, @NonNull Position position) {
        assertFragmentManager();

        getFragmentManager().beginTransaction()
                .add(getOffScreenView().getId(), newFragment)
                .commit();

        getOffScreenView().setVisibility(VISIBLE);

        if (decor != null) {
            decor.onSwipeBegan();

            CharSequence title = adapter.getFragmentTitle(newFragment);
            decor.onSetOffScreenTitle(title);

            // Emulate the very beginning of a swipe so
            // the decor knows what direction to animate
            decor.onSwipeMoved(position == Position.BEFORE ? 0.01f : -0f);
        }

        finishSwipe(position, true, Animation.DURATION_FAST);
    }

    public void setAnimatorContext(@Nullable AnimatorContext animatorContext) {
        this.animatorContext = animatorContext;
    }

    public void setDecor(@Nullable Decor decor) {
        this.decor = decor;

        if (decor != null && adapter != null) {
            TFragment currentFragment = getCurrentFragment();
            if (currentFragment != null) {
                CharSequence title = adapter.getFragmentTitle(currentFragment);
                decor.onSetOnScreenTitle(title);
            }
        }
    }

    @Override
    public boolean hasActiveGesture() {
        return trackingTouchEvents;
    }

    public boolean isAnimating() {
        return (runningAnimation != RunningAnimation.NONE);
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
        if (rawViewX == 0f) {
            return false;
        }

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
            @SuppressLint("CommitTransaction")
            FragmentTransaction transaction = getFragmentManager().beginTransaction().remove(offScreen);
            if (getStateSafeExecutor() != null) {
                getStateSafeExecutor().execute(transaction::commit);
            } else {
                transaction.commit();
            }
        }

        getOffScreenView().setVisibility(INVISIBLE);
    }

    private TFragment addOffScreenFragment(Position position) {
        TFragment newFragment;
        switch (position) {
            case BEFORE:
                newFragment = adapter.getFragmentBeforeFragment(getCurrentFragment());
                break;

            case AFTER:
                newFragment = adapter.getFragmentAfterFragment(getCurrentFragment());
                break;

            default:
                throw new IllegalArgumentException("Unknown position " + position);
        }


        @SuppressLint("CommitTransaction")
        FragmentTransaction transaction = getFragmentManager().beginTransaction()
                .add(getOffScreenView().getId(), newFragment);
        if (getStateSafeExecutor() != null) {
            getStateSafeExecutor().execute(transaction::commit);
        } else {
            transaction.commit();
        }

        getOffScreenView().setVisibility(VISIBLE);

        return newFragment;
    }

    private void exchangeOnAndOffScreen() {
        viewsSwapped = !viewsSwapped;

        removeOffScreenFragment();
        getOnScreenView().setX(0f);
    }

    private void finishSwipe(Position position, boolean fadeOutOnScreen, long duration) {
        PropertyAnimatorProxy onScreenViewAnimator = PropertyAnimatorProxy.animate(getOnScreenView(), animatorContext);
        animationConfig.apply(onScreenViewAnimator).setDuration(duration);
        PropertyAnimatorProxy offScreenViewAnimator = PropertyAnimatorProxy.animate(getOffScreenView(), animatorContext);
        animationConfig.apply(offScreenViewAnimator).setDuration(duration);

        offScreenViewAnimator.x(0f);
        onScreenViewAnimator.x(position == Position.BEFORE ? viewWidth : -viewWidth);
        if (fadeOutOnScreen) {
            onScreenViewAnimator.alpha(0f);
        }

        if (decor != null) {
            decor.onSwipeCompleted(duration, animationConfig, animatorContext);
        }

        onScreenViewAnimator.addOnAnimationCompleted(finished -> {
            if (fadeOutOnScreen) {
                getOnScreenView().setAlpha(1f);
            }

            if (!finished) {
                return;
            }

            Runnable finish = () -> {
                exchangeOnAndOffScreen();

                if (getOnTransitionObserver() != null) {
                    getOnTransitionObserver().onDidTransitionToFragment(getCurrentFragment(), true);
                }

                this.runningAnimation = RunningAnimation.NONE;
            };
            if (stateSafeExecutor != null) {
                stateSafeExecutor.execute(finish);
            } else {
                finish.run();
            }
        });

        if (getOnTransitionObserver() != null) {
            getOnTransitionObserver().onWillTransitionToFragment(getOffScreenFragment(), true);
        }

        onScreenViewAnimator.start();
        offScreenViewAnimator.start();

        this.runningAnimation = RunningAnimation.FINISH_SWIPE;
    }

    private void snapBack(Position position, long duration) {
        PropertyAnimatorProxy onScreenViewAnimator = PropertyAnimatorProxy.animate(getOnScreenView(), animatorContext);
        animationConfig.apply(onScreenViewAnimator).setDuration(duration);
        PropertyAnimatorProxy offScreenViewAnimator = PropertyAnimatorProxy.animate(getOffScreenView(), animatorContext);
        animationConfig.apply(offScreenViewAnimator).setDuration(duration);

        if (decor != null) {
            decor.onSwipeSnappedBack(duration, animationConfig, animatorContext);
        }

        offScreenViewAnimator.x(position == Position.BEFORE ? -viewWidth : viewWidth);
        onScreenViewAnimator.x(0f);
        onScreenViewAnimator.addOnAnimationCompleted(finished -> {
            if (!finished) {
                return;
            }

            Runnable finish = () -> {
                getOnScreenView().setX(0f);
                removeOffScreenFragment();

                if (getOnTransitionObserver() != null) {
                    getOnTransitionObserver().onDidSnapBackToFragment(getCurrentFragment());
                }

                this.runningAnimation = RunningAnimation.NONE;
            };
            if (stateSafeExecutor != null) {
                stateSafeExecutor.execute(finish);
            } else {
                finish.run();
            }
        });

        onScreenViewAnimator.start();
        offScreenViewAnimator.start();

        this.runningAnimation = RunningAnimation.SNAP_BACK;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                if (trackingTouchEvents) {
                    float x = event.getRawX(),
                          y = event.getRawY();
                    float deltaX = x - lastEventX;

                    if (velocityTracker == null) {
                        this.velocityTracker = VelocityTracker.obtain();
                    }
                    velocityTracker.addMovement(event);

                    if (Math.abs(y - lastEventY) < touchSlop) {
                        float newX = viewX + deltaX;
                        Position position = newX > 0.0 ? Position.BEFORE : Position.AFTER;
                        if (position != currentPosition) {
                            removeOffScreenFragment();

                            if (isPositionValid(position)) {
                                TFragment newFragment = addOffScreenFragment(position);
                                if (decor != null) {
                                    CharSequence title = adapter.getFragmentTitle(newFragment);
                                    decor.onSetOffScreenTitle(title);
                                }
                            }

                            this.currentPosition = position;
                        }

                        if (isPositionValid(position)) {
                            getOffScreenView().setX(position == Position.BEFORE ? newX - viewWidth : newX + viewWidth);
                            getOnScreenView().setX(newX);

                            if (decor != null) {
                                float frameValue = newX / viewWidth;
                                decor.onSwipeMoved(frameValue);
                            }

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
                if (trackingTouchEvents) {
                    float velocity = 0f;
                    long duration = 0;
                    if (velocityTracker != null) {
                        velocityTracker.computeCurrentVelocity(1000);

                        velocity = velocityTracker.getXVelocity();
                        duration = Animation.calculateDuration(Math.abs(velocity), getMeasuredWidth());

                        velocityTracker.recycle();
                        this.velocityTracker = null;
                    }


                    if (shouldCompleteTransition(viewX, velocity)) {
                        finishSwipe(currentPosition, false, duration);
                    } else {
                        snapBack(currentPosition, duration);
                    }

                    boolean shouldInvalidate = false;
                    if (!leftEdgeEffect.isFinished()) {
                        leftEdgeEffect.onRelease();

                        shouldInvalidate = true;
                    }

                    if (!rightEdgeEffect.isFinished()) {
                        rightEdgeEffect.onRelease();

                        shouldInvalidate = true;
                    }

                    if (shouldInvalidate) {
                        invalidate();
                    }

                    if (animatorContext != null) {
                        animatorContext.endAnimation();
                    }

                    this.trackingTouchEvents = false;

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
                if (runningAnimation != RunningAnimation.NONE) {
                    if (animatorContext != null) {
                        animatorContext.beginAnimation();
                    }

                    PropertyAnimatorProxy.stop(getOnScreenView(), getOffScreenView());
                    if (decor != null) {
                        decor.onSwipeConclusionInterrupted();
                    }
                    this.trackingTouchEvents = true;
                } else {
                    this.lastEventX = event.getRawX();
                    this.lastEventY = event.getRawY();
                    this.viewX = getOnScreenView().getX();
                    this.viewWidth = getOnScreenView().getMeasuredWidth();

                    TFragment currentFragment = getCurrentFragment();
                    if (currentFragment != null) {
                        this.hasBeforeView = adapter.hasFragmentBeforeFragment(currentFragment);
                        this.hasAfterView = adapter.hasFragmentAfterFragment(currentFragment);
                    } else {
                        this.hasBeforeView = false;
                        this.hasAfterView = false;
                    }
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (runningAnimation != RunningAnimation.NONE && trackingTouchEvents) {
                    this.runningAnimation = RunningAnimation.NONE;

                    return true;
                }

                float x = event.getRawX(), y = event.getRawY();
                float deltaX = x - lastEventX;
                float deltaY = y - lastEventY;
                if (!trackingTouchEvents && Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY)) {
                    this.trackingTouchEvents = true;
                    this.currentPosition = null;

                    if (decor != null) {
                        decor.onSwipeBegan();
                    }

                    if (animatorContext != null) {
                        animatorContext.beginAnimation();
                    }

                    return true;
                }

                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (runningAnimation != RunningAnimation.NONE) {
                    if (runningAnimation == RunningAnimation.FINISH_SWIPE) {
                        finishSwipe(currentPosition, false, Animation.DURATION_FAST);
                    } else if (runningAnimation == RunningAnimation.SNAP_BACK) {
                        snapBack(currentPosition, Animation.DURATION_FAST);
                    }

                    if (animatorContext != null) {
                        animatorContext.endAnimation();
                    }
                }

                this.trackingTouchEvents = false;
                break;
            }
        }

        return false;
    }

    //endregion


    //region Scrolling

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean canScrollHorizontally(int direction) {
        TFragment currentFragment = getCurrentFragment();
        if (adapter == null || currentFragment == null) {
            return false;
        }

        if (direction > 0) {
            return adapter.hasFragmentAfterFragment(currentFragment);
        } else if (direction < 0) {
            return adapter.hasFragmentBeforeFragment(currentFragment);
        } else {
            return false;
        }
    }

    public boolean canScroll() {
        TFragment currentFragment = getCurrentFragment();
        return (currentFragment != null &&
                adapter != null &&
                (adapter.hasFragmentBeforeFragment(currentFragment) ||
                        adapter.hasFragmentAfterFragment(currentFragment)));
    }

    public boolean scrollBackward() {
        TFragment currentFragment = getCurrentFragment();
        if (currentFragment == null || !adapter.hasFragmentBeforeFragment(currentFragment)) {
            return false;
        }

        TFragment newFragment = adapter.getFragmentBeforeFragment(currentFragment);
        setCurrentFragment(newFragment);

        playSoundEffect(SoundEffectConstants.getContantForFocusDirection(FOCUS_BACKWARD));

        return true;
    }

    public boolean scrollForward() {
        TFragment currentFragment = getCurrentFragment();
        if (currentFragment == null || !adapter.hasFragmentAfterFragment(currentFragment)) {
            return false;
        }

        TFragment newFragment = adapter.getFragmentAfterFragment(currentFragment);
        setCurrentFragment(newFragment);

        playSoundEffect(SoundEffectConstants.getContantForFocusDirection(FOCUS_FORWARD));

        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (super.dispatchKeyEvent(event)) {
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT: {
                    return scrollBackward();
                }

                case KeyEvent.KEYCODE_DPAD_RIGHT: {
                    return scrollForward();
                }

                default: {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    //endregion


    //region Accessibility

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            return super.dispatchPopulateAccessibilityEvent(event);
        }

        return getOnScreenView().dispatchPopulateAccessibilityEvent(event);
    }

    private class AccessibilityDelegate extends AccessibilityDelegateCompat {
        @Override
        public void onInitializeAccessibilityEvent(@NonNull View host, @NonNull AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            event.setClassName(FragmentPageView.class.getName());
            AccessibilityRecordCompat record = AccessibilityRecordCompat.obtain();
            record.setScrollable(canScroll());
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(FragmentPageView.class.getName());
            info.setScrollable(canScroll());
            if (canScrollHorizontally(1)) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
            if (canScrollHorizontally(-1)) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
            }
        }

        @Override
        public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
            if (super.performAccessibilityAction(host, action, args)) {
                return true;
            }

            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD: {
                    if (canScrollHorizontally(1)) {
                        scrollForward();
                        return true;
                    }
                }

                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD: {
                    if (canScrollHorizontally(-1)) {
                        scrollBackward();
                        return true;
                    }
                }

                default: {
                    break;
                }
            }
            return false;
        }
    }

    //endregion


    public interface Adapter<TFragment extends Fragment> {
        boolean hasFragmentBeforeFragment(@NonNull TFragment fragment);
        TFragment getFragmentBeforeFragment(@NonNull TFragment fragment);

        boolean hasFragmentAfterFragment(@NonNull TFragment fragment);
        TFragment getFragmentAfterFragment(@NonNull TFragment fragment);

        @Nullable CharSequence getFragmentTitle(@NonNull TFragment fragment);
    }

    public interface OnTransitionObserver<TFragment extends Fragment> {
        void onWillTransitionToFragment(@NonNull TFragment fragment, boolean isInteractive);
        void onDidTransitionToFragment(@NonNull TFragment fragment, boolean isInteractive);
        void onDidSnapBackToFragment(@NonNull TFragment fragment);
    }

    public interface Decor {
        void onSetOnScreenTitle(@Nullable CharSequence title);
        void onSetOffScreenTitle(@Nullable CharSequence title);

        void onSwipeBegan();
        void onSwipeMoved(float newAmount);
        void onSwipeSnappedBack(long duration,
                                @NonNull AnimatorConfig animatorConfig,
                                @Nullable AnimatorContext animatorContext);
        void onSwipeCompleted(long duration,
                              @NonNull AnimatorConfig animatorConfig,
                              @Nullable AnimatorContext animatorContext);
        void onSwipeConclusionInterrupted();
    }

    public enum Position {
        BEFORE,
        AFTER,
    }

    private enum RunningAnimation {
        NONE,
        FINISH_SWIPE,
        SNAP_BACK
    }
}
