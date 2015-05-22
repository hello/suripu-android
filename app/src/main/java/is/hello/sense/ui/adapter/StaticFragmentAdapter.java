package is.hello.sense.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;

public class StaticFragmentAdapter extends FragmentPagerAdapter {
    private final Item[] items;

    public StaticFragmentAdapter(@NonNull FragmentManager fm,
                                 @NonNull Item... items) {
        super(fm);

        this.items = items;
    }

    @Override
    public Fragment getItem(int position) {
        return items[position].newInstance();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return items[position].title;
    }

    @Override
    public int getCount() {
        return items.length;
    }


    public static class Item {
        public final Class<? extends Fragment> fragmentClass;
        public final String title;

        public Item(@NonNull Class<? extends Fragment> fragmentClass,
                    @NonNull String title) {
            this.fragmentClass = fragmentClass;
            this.title = title;
        }


        public @NonNull Fragment newInstance() {
            try {
                return fragmentClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
