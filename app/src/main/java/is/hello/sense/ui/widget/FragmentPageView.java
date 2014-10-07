package is.hello.sense.ui.widget;

import android.animation.Animator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;

import is.hello.sense.R;

@SuppressWarnings("UnusedDeclaration")
public final class FragmentPageView extends FrameLayout {
    private Adapter adapter;
    private FrameLayout view1;
    private FrameLayout view2;
    private boolean viewsSwapped = false;

    private FragmentManager fragmentManager;
    private Fragment currentFragment;

    public FragmentPageView(Context context) {
        super(context);
        initialize();
    }

    public FragmentPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public FragmentPageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }


    //region Properties

    protected void assertFragmentManager() {
        if (fragmentManager == null)
            throw new IllegalStateException(getClass().getSimpleName() + " requires a fragment manager to operate.");
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public Fragment getCurrentFragment() {
        return currentFragment;
    }

    public void setCurrentFragment(Fragment currentFragment) {
        assertFragmentManager();

        if (this.currentFragment != null) {
            getFragmentManager().beginTransaction()
                                .replace(getOnScreenView().getId(), currentFragment)
                                .commit();
        } else {
            getFragmentManager().beginTransaction()
                                .add(getOnScreenView().getId(), currentFragment)
                                .commit();
        }

        this.currentFragment = currentFragment;
    }

    //endregion

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


    //region Events

    private int touchSlop;
    private int viewWidth;
    private float initialX, initialViewX;
    private boolean hasBeforeView = false, hasAfterView = false;
    private Side lastSide;
    private boolean touchEventsTracked = false;

    private void updateOffscreenView(Side side, float currentViewX) {
        switch (side) {
            case LEFT:
                getOffScreenView().setX(currentViewX - viewWidth);
                break;

            case RIGHT:
                getOffScreenView().setX(currentViewX + viewWidth);
                break;
        }
    }

    private void clearOffscreenViewChild() {
        Fragment offScreen = getFragmentManager().findFragmentById(getOffScreenView().getId());
        if (offScreen != null) {
            getFragmentManager().beginTransaction()
                                .remove(offScreen)
                                .commit();
        }
    }

    private void injectOffscreenViewChild(Side side) {
        Fragment newFragment = null;
        switch (side) {
            case LEFT:
                newFragment = adapter.getFragmentBeforeFragment(currentFragment);
                break;

            case RIGHT:
                newFragment = adapter.getFragmentAfterFragment(currentFragment);
                break;
        }

        getFragmentManager().beginTransaction()
                            .add(getOffScreenView().getId(), newFragment)
                            .commit();
    }

    private void exchangeOnAndOffScreen() {
        viewsSwapped = !viewsSwapped;

        this.currentFragment = getFragmentManager().findFragmentById(getOnScreenView().getId());

        clearOffscreenViewChild();
        removeView(getOffScreenView());
        getOnScreenView().setX(0f);

        requestLayout();
    }

    private void completeTransition(Side side) {
        ViewPropertyAnimator currentViewAnimator = getOnScreenView().animate();
        ViewPropertyAnimator offscreenViewAnimator = getOffScreenView().animate();

        offscreenViewAnimator.x(0f);
        switch (side) {
            case LEFT:
                currentViewAnimator.x(viewWidth);
                break;

            case RIGHT:
                currentViewAnimator.x(-viewWidth);
                break;
        }

        currentViewAnimator.setListener(new Animator.AnimatorListener() {
            boolean animationEnded = false;

            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (animationEnded)
                    return;

                animationEnded = true;

                touchEventsTracked = false;
                lastSide = null;

                Log.i("events", "onAnimationEnd");
                clearAnimation();
                exchangeOnAndOffScreen();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        currentViewAnimator.start();
        offscreenViewAnimator.start();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.initialX = event.getX();
                this.initialViewX = getOnScreenView().getX();
                this.viewWidth = getOnScreenView().getMeasuredWidth();

                this.hasBeforeView = adapter.hasFragmentBeforeFragment(currentFragment);
                this.hasAfterView = adapter.hasFragmentAfterFragment(currentFragment);

                Log.i("events", "touch down");

                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                float x = event.getX();
                float deltaX = x - initialX;
                if (!touchEventsTracked && Math.abs(deltaX) > touchSlop) {
                    addView(getOffScreenView());

                    this.touchEventsTracked = true;
                }

                Log.i("events", "touch moved; deltaX: " + deltaX + "; touchEventsTracked: " + touchEventsTracked);

                if (touchEventsTracked) {
                    float newX = initialViewX + deltaX;
                    Side side = newX > 0.0 ? Side.LEFT : Side.RIGHT;
                    if (side != lastSide) {
                        clearOffscreenViewChild();

                        if ((side == Side.LEFT && !hasBeforeView) ||
                            (side == Side.RIGHT && !hasAfterView)) {
                            return true;
                        }

                        injectOffscreenViewChild(side);
                    }

                    updateOffscreenView(side, newX);
                    getOnScreenView().setX(newX);

                    this.lastSide = side;

                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_UP: {
                Log.i("events", "touch up");

                if (touchEventsTracked) {
                    completeTransition(lastSide);

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
        this.view2 = new FrameLayout(getContext());
        view2.setId(R.id.fragment_page_view_off_screen);

        this.view1 = new FrameLayout(getContext());
        view1.setId(R.id.fragment_page_view_on_screen);
        addView(view1);
    }


    public interface Adapter {
        boolean hasFragmentBeforeFragment(@NonNull Fragment fragment);
        Fragment getFragmentBeforeFragment(@NonNull Fragment fragment);

        boolean hasFragmentAfterFragment(@NonNull Fragment fragment);
        Fragment getFragmentAfterFragment(@NonNull Fragment fragment);
    }

    private static enum Side {
        LEFT,
        RIGHT,
    }
}
