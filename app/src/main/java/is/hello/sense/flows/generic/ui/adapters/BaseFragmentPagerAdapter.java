package is.hello.sense.flows.generic.ui.adapters;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.util.Constants;

public abstract class BaseFragmentPagerAdapter extends FragmentPagerAdapter {
    private static final String KEY_LAST_POSITION = BaseFragmentPagerAdapter.class.getSimpleName() + ".KEY_LAST_POSITION";

    private final FragmentManager fragmentManager;
    private final int containerId;

    protected int lastPosition = Constants.NONE;

    private static String makeFragmentName(final int viewId,
                                           final long id) {
        return "android:switcher:" + viewId + ":" + id;
    }

    public BaseFragmentPagerAdapter(final FragmentManager fragmentManager,
                                    final int containerId) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
        this.containerId = containerId;

    }

    @Override
    public void setPrimaryItem(final ViewGroup container,
                               final int position,
                               final Object object) {
        if (lastPosition == position) {
            return;
        }
        alertFragmentVisible(lastPosition, false);
        lastPosition = position;
        super.setPrimaryItem(container, lastPosition, object);
        alertFragmentVisible(lastPosition, true);
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

    private void alertFragmentVisible(final int position,
                                      final boolean isVisible) {
        final Fragment fragment = getFragment(position);
        if (!(fragment instanceof StaticFragmentAdapter.Controller)) {
            return;
        }
        final StaticFragmentAdapter.Controller controller = (StaticFragmentAdapter.Controller) fragment;
        if (!controller.hasPresenterView()) {
            return;
        }
        controller.setVisibleToUser(isVisible);
    }

    @Nullable
    public Fragment getFragment(final int id) {
        return fragmentManager.findFragmentByTag(makeFragmentName(containerId, id));
    }

    @Nullable
    public Fragment getCurrentFragment() {
        if (lastPosition == Constants.NONE) {
            return null;
        }
        return fragmentManager.findFragmentByTag(makeFragmentName(containerId, lastPosition));
    }

    public void onResume() {
        alertFragmentVisible(lastPosition, true);
    }

    public void onPause() {
        alertFragmentVisible(lastPosition, false);
    }

    public interface Controller {
        void setVisibleToUser(boolean isVisible);

        boolean hasPresenterView();

        boolean isVisibleToUser();


    }
}
