package is.hello.sense.mvp.util;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

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
 */
public class ViewPagerPresenterChildDelegate implements ViewPagerPresenterChild {

    /**
     * Should be the {@link is.hello.sense.mvp.presenters.PresenterFragment} holding a reference to this.
     */
    @NonNull
    private final ViewPagerPresenterChild host;
    /**
     * Tracks the state of the fragments visibility. Not to be confused with {@link Fragment#isVisible()}.
     * That function will return true even if this fragment is an adjacent fragment.
     */
    private boolean isVisibleToUser = false;

    /**
     * Tracks the state of the host fragment. Should be falase until {@link is.hello.sense.mvp.presenters.PresenterFragment#presenterView}
     * has been initialized.
     */
    private boolean isViewInitialized = false;

    /**
     * @param host the {@link is.hello.sense.mvp.presenters.PresenterFragment} holding a reference
     *             to this delegate.
     */
    public ViewPagerPresenterChildDelegate(@NonNull final ViewPagerPresenterChild host) {
        this.host = host;
    }

    /**
     * This will be called once before the fragment is created. We only care if {@link #isViewInitialized}
     * is true. We can also use this variable to assume interactors are injected as well.
     *
     * @param isVisibleToUser true when this fragment is the current view in the ViewPager.
     */
    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        this.isVisibleToUser = isVisibleToUser;
        if (!isViewInitialized) {
            return;
        }
        if (isVisibleToUser) {
            onUserVisible();
        } else {
            onUserInvisible();
        }
    }

    @Override
    public void onResume() {
        if (isVisibleToUser) {
            onUserVisible();
        }
    }

    /**
     * Remember that {@link #setUserVisibleHint(boolean)} is NOT called when the app is put in the
     * background. This means it's possible for onPause to be called and {@link #isVisibleToUser} to
     * be true.
     */
    @Override
    public void onPause() {
        // If the phone is being rotated a lot there is a chance presenterView won't be initialized.
        if (isViewInitialized) {
            onUserInvisible();
        }
    }

    /**
     * Called only when the fragment is the current view of its containing ViewPager AND
     * {@link is.hello.sense.mvp.presenters.PresenterFragment#presenterView} is initialized.
     * <p>
     * Anything that depends on this view being the current screen should be called here.
     * Example: polling.
     */
    @Override
    public void onUserVisible() {
        host.onUserVisible();
    }

    /**
     * Called only when the fragment is no longer the focus of its container ViewPager AND
     * {@link is.hello.sense.mvp.presenters.PresenterFragment#presenterView}  is initialized.
     * <p>
     * Anything that depends on this view being the current screen should be stopped here.
     * Example: polling.
     */
    @Override
    public void onUserInvisible() {
        host.onUserInvisible();
    }

    /**
     * Should be called from the {@link #host} during
     * {@link is.hello.sense.mvp.presenters.PresenterFragment#onViewCreated(View, Bundle)}
     */
    public void onViewInitialized() {
        this.isViewInitialized = true;
    }

    public boolean isVisibleToUser(){
        return isVisibleToUser;
    }

}
