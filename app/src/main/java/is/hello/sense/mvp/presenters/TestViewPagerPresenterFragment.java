package is.hello.sense.mvp.presenters;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.fragments.sounds.SleepSoundsFragment;
import is.hello.sense.ui.fragments.sounds.SmartAlarmListFragment;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;

/**
 * This is an example of {@link SleepSoundsFragment} if it were to use {@link ViewPagerPresenterFragment}
 * instead
 */
//todo delete after testing.
public class TestViewPagerPresenterFragment extends ViewPagerPresenterFragment {
    @Override
    public void onRefresh() {

    }


    @NonNull
    @Override
    public StaticFragmentAdapter.Item[] getViewPagerItems() {
        return new StaticFragmentAdapter.Item[]{
                new StaticFragmentAdapter.Item(SmartAlarmListFragment.class, getString(R.string.alarm_subnavbar_alarm_list)),
                new StaticFragmentAdapter.Item(SleepSoundsFragment.class, getString(R.string.alarm_subnavbar_sounds_list))
        };
    }

    @NonNull
    @Override
    public SelectorView.Option[] getSelectorViewOptions() {
        return new SelectorView.Option[]{
                new SelectorView.Option(R.string.alarm_subnavbar_alarm_list, false),
                new SelectorView.Option(R.string.alarm_subnavbar_sounds_list, false)
        };
    }

    @NonNull
    @Override
    public Drawable getSelectorViewBackground() {
        return new TabsBackgroundDrawable(getActivity().getResources(),
                                          TabsBackgroundDrawable.Style.SUBNAV);
    }
}
