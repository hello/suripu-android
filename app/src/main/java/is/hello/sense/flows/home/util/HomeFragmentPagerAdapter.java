package is.hello.sense.flows.home.util;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;

public class HomeFragmentPagerAdapter extends StaticFragmentAdapter {

    private final HomeItem[] homeItems;

    public HomeFragmentPagerAdapter(
            @NonNull final FragmentManager fm,
            final int containerId,
            @NonNull final HomeItem... items) {
        super(fm, containerId, items);
        this.homeItems = items;
    }

    public HomeItem[] getHomeItems() {
        return homeItems;
    }

    public static class HomeItem extends Item {

        public final int normalIcon;
        public final int activeIcon;

        public HomeItem(@NonNull final Class<? extends Fragment> fragmentClass,
                        @NonNull final String title,
                        @DrawableRes final int normalIcon,
                        @DrawableRes final int activeIcon) {
            super(fragmentClass, title);
            this.normalIcon = normalIcon;
            this.activeIcon = activeIcon;
        }
    }
}
