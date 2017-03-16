package is.hello.sense.flows.home.util;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;

public class HomeFragmentPagerAdapter extends StaticFragmentAdapter {

    private final HomeItem[] homeItems;
    private final int timelineItemPosition;

    public HomeFragmentPagerAdapter(
            @NonNull final FragmentManager fm,
            final int timelineItemPosition,
            @NonNull final HomeItem... items) {
        super(fm, items);
        this.homeItems = items;
        this.timelineItemPosition = timelineItemPosition;
    }

    public HomeItem[] getHomeItems() {
        return homeItems;
    }

    public int getTimelineItemPosition() {
        return timelineItemPosition;
    }

    public static class HomeItem extends Item {

        public final int normalIconRes;
        public final int activeIconRes;

        public HomeItem(@NonNull final Class<? extends Fragment> fragmentClass,
                        @NonNull final String title,
                        @DrawableRes final int normalIconRes,
                        @DrawableRes final int activeIconRes) {
            super(fragmentClass, title);
            this.normalIconRes = normalIconRes;
            this.activeIconRes = activeIconRes;
        }
    }
}
