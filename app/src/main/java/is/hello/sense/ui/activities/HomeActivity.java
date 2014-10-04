package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.TimelineFragment;

public class HomeActivity extends InjectionActivity {
    private final String SAVED_VIEW_PAGER_STATE = HomeActivity.class.getName() + ".SAVED_VIEW_PAGER_STATE";

    private TimelineAdapter timelineAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ViewPager viewPager = (ViewPager) findViewById(R.id.activity_home_view_pager);

        this.timelineAdapter = new TimelineAdapter(getSupportFragmentManager(), new DateTime(2014, 9, 22, 12, 0));
        viewPager.setAdapter(timelineAdapter);

        if (savedInstanceState == null) {
            viewPager.setCurrentItem(timelineAdapter.numberOfDays - 1, false);
        } else {
            timelineAdapter.restoreState(savedInstanceState.getParcelable(SAVED_VIEW_PAGER_STATE), TimelineAdapter.class.getClassLoader());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Parcelable adapterState = timelineAdapter.saveState();
        outState.putParcelable(SAVED_VIEW_PAGER_STATE, adapterState);
    }

    private class TimelineAdapter extends FragmentPagerAdapter {
        // This is a temporary workaround. The real time line navigation system
        // will need to support infinite navigation both forwards and backwards.

        public final DateTime seedTime;
        public final int numberOfDays;

        private TimelineAdapter(@NonNull FragmentManager fm, @NonNull DateTime seedTime) {
            super(fm);

            this.seedTime = seedTime;
            this.numberOfDays = seedTime.getDayOfYear();
        }


        @Override
        public int getCount() {
            return numberOfDays;
        }

        @Override
        public Fragment getItem(int position) {
            DateTime date = seedTime.minusDays(numberOfDays - position);
            return TimelineFragment.newInstance(date);
        }
    }
}
