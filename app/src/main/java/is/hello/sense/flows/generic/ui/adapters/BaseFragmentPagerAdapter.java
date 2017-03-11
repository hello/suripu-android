package is.hello.sense.flows.generic.ui.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import is.hello.sense.util.Constants;

/**
 * Keeps track of last object and its position to notify when visible to user
 */

public abstract class BaseFragmentPagerAdapter extends FragmentPagerAdapter {

    private static final String KEY_LAST_POSITION = BaseFragmentPagerAdapter.class.getSimpleName() + ".KEY_LAST_POSITION";
    private int lastPosition = Constants.NONE;
    private int containerId = Constants.NONE;
    private final FragmentManager fm;
    @Nullable
    private Object currentObject;

    private static String getFragmentTag(final int viewId,
                                           final long id) {
        return "android:switcher:" + viewId + ":" + id;
    }

    public BaseFragmentPagerAdapter(@NonNull final FragmentManager fm) {
        super(fm);
        this.fm = fm;
    }

    @Override
    public void setPrimaryItem(final ViewGroup container,
                               final int position,
                               final Object object) {
        this.containerId = container.getId();
        if (lastPosition == position) {
            return;
        }
        alertFragmentVisible(containerId, lastPosition, false);
        this.lastPosition = position;
        this.currentObject = object;
        super.setPrimaryItem(container, lastPosition, object);
        alertFragmentVisible(containerId, lastPosition, true);
    }

    @Override
    public void destroyItem(final ViewGroup container,
                            final int position,
                            final Object object) {
        super.destroyItem(container, position, object);
        if(currentObject == object) {
            this.currentObject = null;
        }
    }

    @Override
    public Parcelable saveState() {
        final Bundle state = new Bundle();
        state.putInt(KEY_LAST_POSITION, lastPosition);
        return state;
    }

    @Override
    public void restoreState(@Nullable final Parcelable state,
                             @Nullable final ClassLoader loader) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            bundle.setClassLoader(loader);
            this.lastPosition = bundle.getInt(KEY_LAST_POSITION, Constants.NONE);
        }
    }

    public int getLastPosition() {
        return lastPosition;
    }

    @Nullable
    public Object getCurrentObject() {
        return currentObject;
    }

    public void onResume() {
        alertFragmentVisible(containerId, lastPosition, true);
    }

    public void onPause() {
        alertFragmentVisible(containerId, lastPosition, false);
    }

    private void alertFragmentVisible(final int viewId,
                                      final int position,
                                      final boolean isVisible) {
        final Fragment fragment = getFragment(viewId, position);
        if (!(fragment instanceof Controller)) {
            return;
        }
        final Controller controller = (Controller) fragment;

        controller.setVisibleToUser(isVisible);
    }

    /**
     * @param containerId of container of fragment
     * @param position of fragment when added in transaction
     * @return fragment if found based on tag
     */
    @Nullable
    private Fragment getFragment(final int containerId,
                                 final int position) {
        return fm.findFragmentByTag(getFragmentTag(containerId,
                                                   getItemId(position)));
    }

    public interface Controller {
        void setVisibleToUser(boolean isVisible);
    }
}
