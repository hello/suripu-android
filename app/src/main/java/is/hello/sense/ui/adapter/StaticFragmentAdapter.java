package is.hello.sense.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;

public class StaticFragmentAdapter extends FragmentPagerAdapter {
    private final Item[] items;

    /**
     * Used to limit the number of items shown.
     */
    private int overrideCount = -1;

    public StaticFragmentAdapter(@NonNull final FragmentManager fm,
                                 @NonNull final Item... items) {
        super(fm);

        this.items = items;
    }

    @Override
    public Fragment getItem(final int position) {
        return items[position].newInstance();
    }

    @Override
    public CharSequence getPageTitle(final int position) {
        return items[position].title;
    }

    @Override
    public int getCount() {
        if (overrideCount == -1) {
            return items.length;
        }
        return overrideCount;

    }

    public void setOverrideCount(int overrideCount) {
        if (overrideCount < -1) {
            overrideCount = -1;
        }
        if (overrideCount > items.length - 1) {
            overrideCount = items.length - 1;
        }
        this.overrideCount = overrideCount;
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
}
