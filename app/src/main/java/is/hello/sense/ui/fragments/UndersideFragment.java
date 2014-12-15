package is.hello.sense.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.fragments.settings.AppSettingsFragment;

import static is.hello.sense.ui.adapter.StaticFragmentAdapter.Item;

public class UndersideFragment extends Fragment {
    private ViewPager pager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_underside, container, false);

        PagerTabStrip tabStrip = (PagerTabStrip) view.findViewById(R.id.fragment_underside_pager_strip);
        tabStrip.setTabIndicatorColorResource(R.color.light_accent);
        tabStrip.setBackgroundResource(R.color.white);
        tabStrip.setTextSpacing(0);
        tabStrip.setGravity(Gravity.START | Gravity.BOTTOM);

        this.pager = (ViewPager) view.findViewById(R.id.fragment_underside_pager);
        pager.setAdapter(new StaticFragmentAdapter(getFragmentManager(),
                new Item(CurrentConditionsFragment.class, getString(R.string.title_current_conditions)),
                new Item(Fragment.class, getString(R.string.title_trends)),
                new Item(InsightsFragment.class, getString(R.string.action_insights)),
                new Item(SmartAlarmListFragment.class, getString(R.string.action_alarm)),
                new Item(AppSettingsFragment.class, getString(R.string.action_settings))
        ));

        return view;
    }


    public boolean isAtStart() {
        return (pager.getCurrentItem() == 0);
    }

    public void jumpToStart() {
        pager.setCurrentItem(0, true);
    }
}
