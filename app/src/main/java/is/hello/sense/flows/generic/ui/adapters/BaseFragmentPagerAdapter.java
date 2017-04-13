package is.hello.sense.flows.generic.ui.adapters;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.mvp.presenters.ControllerPresenterFragment;
import is.hello.sense.util.Constants;

import android.support.v4.view.ViewPager;

/**
 * Use this adapter with a {@link ViewPager} that will be showing multiple {@link Fragment}.
 * <p>
 * Each Fragment used should implement {@link Controller} for better control over knowing when it is
 * visible or invisible to the user.  This adapter takes responsibility for telling the fragment
 * when it has become visible or invisible by calling {@link Controller#setVisibleToUser(boolean)}.
 * <p>
 * You can extend {@link ControllerPresenterFragment} which already implements the Controller interface
 * and has a helper instance field tracking if the fragment is visible or not. Read the field by using
 * {@link ControllerPresenterFragment#isVisibleToUser()}.
 */
public abstract class BaseFragmentPagerAdapter extends FragmentPagerAdapter {
    private static final String KEY_LAST_POSITION = BaseFragmentPagerAdapter.class.getSimpleName() + ".KEY_LAST_POSITION";
    private static final String KEY_CONTAINER_ID = BaseFragmentPagerAdapter.class.getSimpleName() + ".KEY_CONTAINER_ID";

    private final FragmentManager fragmentManager;
    /**
     * Containing views ID.
     */
    private int containerId = View.NO_ID;
    /**
     * This is very important for {@link #setPrimaryItem(ViewGroup, int, Object)} and
     * {@link #findFragment(int)}.
     */
    protected int lastPosition = Constants.NONE;

    /**
     * When true will do an additional call to {@link #alertFragmentVisible(int, boolean)} with the
     * currently shown lastPosition. Important for scheduled night mode to recreate and select
     * the currently visible fragment.
     * }
     */
    private boolean forceAlertFragment = false;

    /**
     * A known hack for mimicking {@link FragmentPagerAdapter#makeFragmentName(int, long)}.
     * <p>
     * This may break in future releases of Android.
     *
     * @param viewId usually the id of the ViewPager displaying the fragments.
     * @param id     this is the position of the fragment. Think of it like an array.
     * @return the tag name created by default in {@link FragmentPagerAdapter#instantiateItem(ViewGroup, int)}
     */
    private static String makeFragmentName(final int viewId,
                                           final long id) {
        return "android:switcher:" + viewId + ":" + id;
    }

    public BaseFragmentPagerAdapter(final FragmentManager fragmentManager) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;

    }

    @Override
    public void startUpdate(final ViewGroup container) {
        super.startUpdate(container);
        this.containerId = container.getId();
    }

    /**
     * This is an important override. By default the adapter can repeatedly call this method which
     * will perform multiple {@link Controller#setVisibleToUser(boolean)} calls. We use {@link #lastPosition}
     * to make sure it's only called once.
     *
     * @param container
     * @param position
     * @param object
     */
    @Override
    public void setPrimaryItem(final ViewGroup container,
                               final int position,
                               final Object object) {
        if (lastPosition == position) {
            /*
                HomeActivity uses onResume to control its first fragment. When tabs are pressed
                alertFragmentVisible alerts fragments of changes. When the HomeActivity recreates
                itself, it will start a fragment (that doesn't normally start on app launch) with
                onResume. This isn't enough to tell the fragment it's visible. forceAlertFragment is
                used as a flag that will force this function to call alertFragmentVisible with the
                starting fragment position.
             */
            if (!forceAlertFragment) {
                return; // skip
            }
            forceAlertFragment = false;
        }


        alertFragmentVisible(lastPosition, false);
        lastPosition = position;
        super.setPrimaryItem(container, lastPosition, object);
        alertFragmentVisible(lastPosition, true);
    }

    @CallSuper
    @Override
    public Parcelable saveState() {
        final Bundle state = new Bundle();
        state.putInt(KEY_LAST_POSITION, lastPosition);
        state.putInt(KEY_CONTAINER_ID, containerId);
        return state;
    }

    @CallSuper
    @Override
    public void restoreState(@Nullable final Parcelable state,
                             @Nullable final ClassLoader loader) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            bundle.setClassLoader(loader);
            this.lastPosition = bundle.getInt(KEY_LAST_POSITION, Constants.NONE);
            this.containerId = bundle.getInt(KEY_CONTAINER_ID, Constants.NONE);
        }
    }

    /**
     * Alerts the fragment at the given position that it is visible or invisible. Will check if
     * the fragment implements {@link Controller} and it will make sure that fragment's PresenterView
     * isn't null.
     *
     * @param position
     * @param isVisible
     */
    public final void alertFragmentVisible(final int position,
                                           final boolean isVisible) {
        final Fragment fragment = findFragment(position);
        if (!(fragment instanceof BaseFragmentPagerAdapter.Controller)) {
            return;
        }
        final BaseFragmentPagerAdapter.Controller controller = (BaseFragmentPagerAdapter.Controller) fragment;
        if (!controller.hasPresenterView()) {
            return;
        }
        controller.setVisibleToUser(isVisible);
    }

    /**
     * This will specifically find an existing fragment. This SHOULD NOT create one. Use
     * {@link #getItem(int)} to create one.
     *
     * @param id position of fragment. Will be used to convert into the correct fragment tag
     *           with {@link #makeFragmentName(int, long)}.
     * @return null if no fragment has been made with that id yet.
     */
    @Nullable
    public final Fragment findFragment(final int id) {
        if (containerId == View.NO_ID) {
            return null;
        }
        return fragmentManager.findFragmentByTag(makeFragmentName(containerId, getItemId(id)));
    }

    /**
     * Returns the current visible fragment.
     *
     * @return null if no fragment exists with id of {{@link #lastPosition}}
     */
    @Nullable
    public final Fragment findCurrentFragment() {
        if (containerId == View.NO_ID) {
            return null;
        }
        if (lastPosition == Constants.NONE) {
            return null;
        }
        return fragmentManager.findFragmentByTag(makeFragmentName(containerId, getItemId(lastPosition)));
    }

    /**
     * In order to strictly rely on the {@link Controller} to determine our user visibility state we
     * need to make sure the Activity/Fragment that created this adapter calls this when it resumes
     * to tell the current Fragment it is now visible.
     */
    public final void onResume() {
        if (lastPosition == Constants.NONE) {
            forceAlertFragment = true;
        }
        alertFragmentVisible(lastPosition, true);
    }

    /**
     * In order to strictly rely on the {@link Controller} to determine our user visibility state we
     * need to make sure the Activity/Fragment that created this adapter calls this when it pauses
     * to tell the current Fragment it is now invisible.
     */
    public final void onPause() {
        forceAlertFragment = false;
        alertFragmentVisible(lastPosition, false);
    }

    public interface Controller {
        void setVisibleToUser(boolean isVisible);

        boolean hasPresenterView();

        boolean isVisibleToUser();


    }
}
