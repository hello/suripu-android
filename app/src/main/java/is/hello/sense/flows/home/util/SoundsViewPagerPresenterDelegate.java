package is.hello.sense.flows.home.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.fragments.SleepSoundsFragment;
import is.hello.sense.flows.home.ui.fragments.SmartAlarmListFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.flows.home.ui.adapters.StaticFragmentAdapter;
import is.hello.sense.util.NotTested;

@NotTested
public class SoundsViewPagerPresenterDelegate extends BaseViewPagerPresenterDelegate {
    private final Resources resources;

    public SoundsViewPagerPresenterDelegate(@NonNull final Resources resources) {
        this.resources = resources;
    }

    @NonNull
    @Override
    public StaticFragmentAdapter.Item[] getViewPagerItems() {
        return new StaticFragmentAdapter.Item[]{
                new StaticFragmentAdapter.Item(SmartAlarmListFragment.class,
                                               resources.getString(R.string.alarm_subnavbar_alarm_list)),
                new StaticFragmentAdapter.Item(SleepSoundsFragment.class,
                                               resources.getString(R.string.alarm_subnavbar_sounds_list))
        };
    }
}
