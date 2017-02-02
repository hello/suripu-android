package is.hello.sense.flows.home.ui.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import is.hello.sense.util.Constants;


public class StaticFragmentAdapter extends FragmentPagerAdapter {
    private static final String KEY_LAST_POSITION = StaticFragmentAdapter.class.getSimpleName() + ".KEY_LAST_POSITION";

    private final Item[] items;
    private int lastPosition = Constants.NONE;
    private final FragmentManager fragmentManager;
    private final int containerId;

    private static String makeFragmentName(final int viewId,
                                           final long id) {
        return "android:switcher:" + viewId + ":" + id;
    }

    public StaticFragmentAdapter(@NonNull final FragmentManager fm,
                                 final int containerId,
                                 @NonNull final Item... items) {
        super(fm);
        this.fragmentManager = fm;
        this.items = items;
        this.containerId = containerId;
    }

    @Nullable
    public Fragment getFragment(final int id) {
        return fragmentManager.findFragmentByTag(makeFragmentName(containerId, id));
    }


    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Fragment getItem(final int position) {
        return items[position].newInstance();
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

    public void restoreSavedInstanceState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        this.lastPosition = savedInstanceState.getInt(KEY_LAST_POSITION, Constants.NONE);
    }

    public void saveInstanceState(@NonNull final Bundle outstate) {
        outstate.putInt(KEY_LAST_POSITION, lastPosition);
    }

    private void alertFragmentVisible(final int position,
                                      final boolean isVisible) {
        final Fragment fragment = getFragment(position);
        if (!(fragment instanceof Controller)) {
            return;
        }
        final Controller controller = (Controller) fragment;
        if (!controller.hasPresenterView()) {
            return;
        }
        controller.setVisibleToUser(isVisible);
    }

    public static class Item {
        public final Class<? extends Fragment> fragmentClass;
        public final String title;

        public Item(@NonNull final Class<? extends Fragment> fragmentClass,
                    @NonNull final String title) {
            this.fragmentClass = fragmentClass;
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        @NonNull
        public Fragment newInstance() {
            try {
                return fragmentClass.newInstance();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Class<? extends Fragment> getFragmentClass() {
            return fragmentClass;
        }
    }

    public interface Controller {
        void setVisibleToUser(boolean isVisible);

        boolean hasPresenterView();

        boolean isVisibleToUser();


    }

}
