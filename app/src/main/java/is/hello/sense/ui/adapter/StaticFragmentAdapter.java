package is.hello.sense.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

import is.hello.sense.mvp.util.ViewPagerPresenterChild;

public class StaticFragmentAdapter extends android.support.v13.app.FragmentStatePagerAdapter {
    private final Item[] items;
    private int lastPosition = -1;
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
        Log.e(getClass().getSimpleName(), "Instance: " + position);
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
        Log.e(getClass().getSimpleName(), "alertFragmentVisible: " + position);
        final Fragment fragment = getFragmentAtPosition(position);
        if (!(fragment instanceof ViewPagerPresenterChild)) {
            return;
        }
/*
        if (isVisible) {
            ((ViewPagerPresenterChild) fragment).onUserVisible();
        } else {
            ((ViewPagerPresenterChild) fragment).onUserInvisible();
        }*/
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

    public static String makeFragmentName(final int viewId, final long id) {
        return "android:switcher:" + viewId + ":" + id;
    }
}
