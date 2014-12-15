package is.hello.sense.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;

import static is.hello.sense.ui.adapter.StaticFragmentAdapter.Item;

public class UndersideFragment extends Fragment {
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

        ViewPager pager = (ViewPager) view.findViewById(R.id.fragment_underside_pager);
        pager.setAdapter(new StaticFragmentAdapter(getFragmentManager(),
                new Item(Fragment.class, "Room"),
                new Item(Fragment.class, "Trends"),
                new Item(InsightsFragment.class, "Insights"),
                new Item(SmartAlarmListFragment.class, "Alarms"),
                new Item(Fragment.class, "Settings")
        ));

        return view;
    }
}
