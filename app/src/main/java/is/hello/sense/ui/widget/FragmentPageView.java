package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.util.Animation;

import static is.hello.sense.util.Animation.PropertyAnimatorProxy;

@SuppressWarnings("UnusedDeclaration")
public final class FragmentPageView<TFragment extends Fragment> extends FrameLayout {
    private Adapter<TFragment> adapter;
    private FrameLayout view1;
    private FrameLayout view2;
    private boolean viewsSwapped = false;

    private FragmentManager fragmentManager;

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

    public Adapter<TFragment> getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter<TFragment> adapter) {
        this.adapter = adapter;
    }

    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public TFragment getCurrentFragment() {
        // noinspection unchecked
        return (TFragment) getFragmentManager().findFragmentById(getOnScreenView().getId());
    }

    public void setCurrentFragment(TFragment currentFragment) {
        assertFragmentManager();

        if (getCurrentFragment() != null) {
            getFragmentManager().beginTransaction()
                    .replace(getOnScreenView().getId(), currentFragment)
                    .commit();
        } else {
            getFragmentManager().beginTransaction()
                    .add(getOnScreenView().getId(), currentFragment)
                    .commit();
        }
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
    private VelocityTracker velocityTracker;

    private int viewWidth;
    private float initialViewX;
    private float lastX, lastY;
    private float viewX;
    private Position currentPosition;
    private boolean hasBeforeView = false, hasAfterView = false;
    private boolean isTrackingTouchEvents = false;

    private boolean isOffScreenViewOverThreshold(Position position) {
        return Math.abs(getOnScreenView().getX()) > viewWidth / 3;
    }

    private void updateOffscreenView(Position position, float currentViewX) {
        switch (position) {
            case BEFORE:
                getOffScreenView().setX(currentViewX - viewWidth);
                break;

            case AFTER:
                getOffScreenView().setX(currentViewX + viewWidth);
                break;
        }
    }

    private void removeOffScreenFragment() {
        Fragment offScreen = getFragmentManager().findFragmentById(getOffScreenView().getId());
        if (offScreen != null) {
            getFragmentManager().beginTransaction()
                    .remove(offScreen)
                    .commit();
        }
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
    }

    private void exchangeOnAndOffScreen() {
        viewsSwapped = !viewsSwapped;

        removeOffScreenFragment();
        removeView(getOffScreenView());
        getOnScreenView().setX(0f);

        requestLayout();
    }

    private void completeTransition(Position position, long duration) {
        PropertyAnimatorProxy onScreenViewAnimator = Animation.animate(getOnScreenView()).setDuration(duration);
        PropertyAnimatorProxy offScreenViewAnimator = Animation.animate(getOffScreenView()).setDuration(duration);

        offScreenViewAnimator.x(0f);
        onScreenViewAnimator.x(position == Position.BEFORE ? viewWidth : -viewWidth);

        onScreenViewAnimator.setOnAnimationCompleted(finished -> {
            if (!finished)
                return;

            this.currentPosition = null;

            exchangeOnAndOffScreen();
        });

        onScreenViewAnimator.start();
        offScreenViewAnimator.start();
    }

    private void snapBack(Position position, long duration) {
        PropertyAnimatorProxy onScreenViewAnimator = Animation.animate(getOnScreenView()).setDuration(duration);
        PropertyAnimatorProxy offScreenViewAnimator = Animation.animate(getOffScreenView()).setDuration(duration);

        offScreenViewAnimator.x(position == Position.BEFORE ? -viewWidth : viewWidth);
        onScreenViewAnimator.x(0f);
        onScreenViewAnimator.setOnAnimationCompleted(finished -> {
            if (!finished)
                return;

            this.currentPosition = null;

            removeOffScreenFragment();
            removeView(getOffScreenView());
            getOnScreenView().setX(0f);
        });

        onScreenViewAnimator.start();
        offScreenViewAnimator.start();
    }

    public final OnTouchListener TOUCH_LISTENER = (view, event) -> {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                clearAnimation();

                this.lastX = event.getRawX();
                this.lastY = event.getRawY();
                this.viewX = getOnScreenView().getX();
                this.initialViewX = viewX;
                this.viewWidth = getOnScreenView().getMeasuredWidth();

                this.hasBeforeView = adapter.hasFragmentBeforeFragment(getCurrentFragment());
                this.hasAfterView = adapter.hasFragmentAfterFragment(getCurrentFragment());

                return true;
            }

            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_MOVE: {
                float x = event.getRawX(), y = event.getRawY();
                float deltaX = x - lastX;
                if (!isTrackingTouchEvents && Math.abs(deltaX) > touchSlop) {
                    if (getOffScreenView().getParent() == null)
                        addView(getOffScreenView());

                    this.velocityTracker = VelocityTracker.obtain();
                    this.isTrackingTouchEvents = true;
                }

                if (isTrackingTouchEvents) {
                    velocityTracker.addMovement(event);

                    if (Math.abs(y - lastY) < touchSlop) {
                        float newX = viewX + deltaX;
                        Position position = newX > 0.0 ? Position.BEFORE : Position.AFTER;
                        if (position != currentPosition) {
                            removeOffScreenFragment();

                            if ((position == Position.BEFORE && !hasBeforeView) || (position == Position.AFTER && !hasAfterView)) {
                                return true;
                            }

                            addOffScreenFragment(position);

                            this.currentPosition = position;
                        }

                        updateOffscreenView(position, newX);
                        getOnScreenView().setX(newX);

                        this.viewX = newX;
                    }

                    this.lastX = x;
                    this.lastY = y;

                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (isTrackingTouchEvents) {
                    velocityTracker.computeCurrentVelocity(1000);

                    float velocity = Math.abs(velocityTracker.getXVelocity());
                    long duration = Math.max(150, Math.min(450, (long) (getMeasuredWidth() / velocity) * 1000 / 2));

                    if (isOffScreenViewOverThreshold(currentPosition))
                        completeTransition(currentPosition, duration);
                    else
                        snapBack(currentPosition, duration);

                    velocityTracker.recycle();
                    this.velocityTracker = null;
                    this.isTrackingTouchEvents = false;

                    return true;
                }

                break;
            }
        }

        return false;
    };

    //endregion


    protected void initialize() {
        this.touchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        this.view2 = new FrameLayout(getContext());
        view2.setId(R.id.fragment_page_view_off_screen);

        this.view1 = new FrameLayout(getContext());
        view1.setId(R.id.fragment_page_view_on_screen);
        addView(view1);

        setOnTouchListener(TOUCH_LISTENER);
    }


    public interface Adapter<TFragment extends Fragment> {
        boolean hasFragmentBeforeFragment(@NonNull TFragment fragment);
        TFragment getFragmentBeforeFragment(@NonNull TFragment fragment);

        boolean hasFragmentAfterFragment(@NonNull TFragment fragment);
        TFragment getFragmentAfterFragment(@NonNull TFragment fragment);
    }

    private static enum Position {
        BEFORE,
        AFTER,
    }
}
