package is.hello.sense.flows.home.ui.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;

import is.hello.sense.flows.generic.ui.adapters.BaseFragmentPagerAdapter;


public class StaticFragmentAdapter extends BaseFragmentPagerAdapter {
    private final Item[] items;

    public StaticFragmentAdapter(@NonNull final FragmentManager fm,
                                 final int containerId,
                                 @NonNull final Item... items) {
        super(fm, containerId);
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Fragment getItem(final int position) {
        return items[position].newInstance();
    }


    public static class Item {
        private final Class<? extends Fragment> fragmentClass;
        private final String title;

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
