package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
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
    private int viewWidth;
    private float lastX, viewX;
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

    private void completeTransition(Position position) {
        PropertyAnimatorProxy currentViewAnimator = Animation.animate(getOnScreenView());
        PropertyAnimatorProxy offscreenViewAnimator = Animation.animate(getOffScreenView());

        offscreenViewAnimator.x(0f);
        currentViewAnimator.x(position == Position.BEFORE? viewWidth : -viewWidth);

        currentViewAnimator.setOnAnimationCompleted(finished -> {
            this.isTrackingTouchEvents = false;
            this.currentPosition = null;

            exchangeOnAndOffScreen();
        });

        currentViewAnimator.start();
        offscreenViewAnimator.start();
    }

    private void snapBack(Position position) {
        PropertyAnimatorProxy currentViewAnimator = Animation.animate(getOnScreenView());
        PropertyAnimatorProxy offscreenViewAnimator = Animation.animate(getOffScreenView());

        offscreenViewAnimator.x(position == Position.BEFORE? -viewWidth : viewWidth);
        currentViewAnimator.x(0f);
        currentViewAnimator.setOnAnimationCompleted(finished -> {
            this.isTrackingTouchEvents = false;
            this.currentPosition = null;

            removeOffScreenFragment();
            removeView(getOffScreenView());
            getOnScreenView().setX(0f);
        });

        currentViewAnimator.start();
        offscreenViewAnimator.start();
    }

    public final OnTouchListener TOUCH_LISTENER = (view, event) -> {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.lastX = event.getX();
                this.viewX = getOnScreenView().getX();
                this.viewWidth = getOnScreenView().getMeasuredWidth();

                this.hasBeforeView = adapter.hasFragmentBeforeFragment(getCurrentFragment());
                this.hasAfterView = adapter.hasFragmentAfterFragment(getCurrentFragment());

                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                float x = event.getX();
                float deltaX = x - lastX;
                if (!isTrackingTouchEvents && Math.abs(deltaX) > touchSlop) {
                    addView(getOffScreenView());

                    this.isTrackingTouchEvents = true;
                }

                if (isTrackingTouchEvents) {
                    float newX = viewX + deltaX;
                    Position position = newX > 0.0 ? Position.BEFORE : Position.AFTER;
                    if (position != currentPosition) {
                        removeOffScreenFragment();

                        if ((position == Position.BEFORE && !hasBeforeView) ||
                                (position == Position.AFTER && !hasAfterView)) {
                            return true;
                        }

                        addOffScreenFragment(position);

                        this.currentPosition = position;
                    }

                    updateOffscreenView(position, newX);
                    this.viewX = newX;
                    getOnScreenView().setX(newX);

                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_UP: {
                if (isTrackingTouchEvents) {
                    if (isOffScreenViewOverThreshold(currentPosition))
                        completeTransition(currentPosition);
                    else
                        snapBack(currentPosition);

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
