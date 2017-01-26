package is.hello.sense.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;


public class StaticFragmentAdapter extends android.support.v13.app.FragmentStatePagerAdapter {
    private final Item[] items;
    private int lastPosition = -1;

    // FragmentStatePagerAdapter find by tag return null
    // Need this to hold references of each fragment.
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    public StaticFragmentAdapter(@NonNull final FragmentManager fm,
                                 @NonNull final Item... items) {
        super(fm);
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Fragment getItem(final int position) {
        final Fragment fragment = items[position].newInstance();
        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public Object instantiateItem(final ViewGroup container,
                                  final int position) {
        final Fragment fragment = ((Fragment) super.instantiateItem(container, position));
        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(final ViewGroup container,
                            final int position,
                            final Object object) {
        fragmentMap.remove(position);
        super.destroyItem(container, position, object);
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

    @Nullable
    public Fragment getFragmentAtPosition(final int position) {
        return fragmentMap.get(position);
    }

    private void alertFragmentVisible(final int position,
                                      final boolean isVisible) {
        final Fragment fragment = getFragmentAtPosition(position);
        if (!(fragment instanceof Controller)) {
            return;
        }
        final Controller controller = (Controller) fragment;
        if (!controller.hasPresenterView()) {
            return;
        }
        if (isVisible) {
            controller.isVisibleToUser();
        } else {
            controller.isInvisibleToUser();
        }
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
    }

    public interface Controller {
        void isVisibleToUser();

        void isInvisibleToUser();

        boolean hasPresenterView();
    }

}
