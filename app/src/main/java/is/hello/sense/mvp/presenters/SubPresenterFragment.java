package is.hello.sense.mvp.presenters;


import android.support.annotation.CallSuper;

import is.hello.sense.mvp.view.PresenterView;

/**
 * Designed for fragments that will exist inside of a ViewPager. Aka any fragment provided/used with
 * {@link is.hello.sense.mvp.util.ViewPagerPresenter}
 * <p>
 * A fragment inside of a ViewPager will NOT call {@link #onResume()} or {@link #onPause()}
 * when swiping/tapping between adjacent fragments. We rely on {@link #setUserVisibleHint(boolean)}
 * to determine visibility in that scenario.
 * <p>
 * When the device screen is shut off or the app put in the background, {@link #onResume()} and
 * {@link #onPause()} are called for the current fragment in the ViewPager and its adjacent fragments.
 * {@link #setUserVisibleHint(boolean)} will not be called for that scenario.
 *
 * @param <T>
 */
public abstract class SubPresenterFragment<T extends PresenterView> extends PresenterFragment<T> {
    /**
     * Tracks the state of the fragments visibility. Not to be confused with {@link #isVisible()}.
     * That function will return true even if this fragment is an adjacent fragment.
     */
    private boolean isVisibleToUser = false;

    /**
     * This will be called once before the fragment is created. We only care if {@link #presenterView}
     * is initialized. We can also use this variable to assume interactors are injected as well.
     *
     * @param isVisibleToUser true when this fragment is the current view in the ViewPager.
     */
    @Override
    public final void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        if (presenterView == null) {
            return;
        }
        if (isVisibleToUser) {
            onUserVisible();
        } else {
            onUserInvisible();
        }
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        if (isVisibleToUser) {
            onUserVisible();
        }
    }

    /**
     * Remember that {@link #setUserVisibleHint(boolean)} is NOT called when the app is put in the
     * background. This means it's possible for onPause to be called and {@link #isVisibleToUser} to
     * be true.
     */
    @CallSuper
    @Override
    public void onPause() {
        super.onPause();
        // If the phone is being rotated a lot there is a chance presenterView won't be initialized.
        if (presenterView != null) {
            onUserInvisible();
        }
    }

    public final boolean isVisibleToUser() {
        return isVisibleToUser;
    }

    public final boolean isVisibleToUserAndResumed() {
        return isResumed() && isVisibleToUser;
    }

    /**
     * Called only when the fragment is the current view of its containing ViewPager AND
     * {@link #presenterView} is initialized.
     * <p>
     * Anything that depends on this view being the current screen should be called here.
     * Example: polling.
     */
    public abstract void onUserVisible();

    /**
     * Called only when the fragment is no longer the focus of its container ViewPager AND
     * {@link #presenterView} is initialized.
     * <p>
     * Anything that depends on this view being the current screen should be stopped here.
     * Example: polling.
     */
    public abstract void onUserInvisible();

}
