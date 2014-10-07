package is.hello.sense.ui.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.FragmentPageView;

public class HomeActivity extends InjectionActivity {
    private final String SAVED_VIEW_PAGER_STATE = HomeActivity.class.getName() + ".SAVED_VIEW_PAGER_STATE";

    private TimelineAdapter timelineAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FragmentPageView viewPager = (FragmentPageView) findViewById(R.id.activity_home_view_pager);
        viewPager.setFragmentManager(getSupportFragmentManager());
        viewPager.setAdapter(new FragmentPageView.Adapter() {
            @Override
            public boolean hasFragmentBeforeFragment(@NonNull Fragment fragment) {
                return true;
            }

            @NonNull
            @Override
            public Fragment getFragmentBeforeFragment(@NonNull Fragment fragment) {
                return new Fragment() {
                    @Override
                    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                        View view = new View(getActivity());
                        view.setBackgroundColor(Color.GREEN);
                        return view;
                    }
                };
            }

            @Override
            public boolean hasFragmentAfterFragment(@NonNull Fragment fragment) {
                return true;
            }

            @NonNull
            @Override
            public Fragment getFragmentAfterFragment(@NonNull Fragment fragment) {
                return new Fragment() {
                    @Override
                    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                        View view = new View(getActivity());
                        view.setBackgroundColor(Color.YELLOW);
                        return view;
                    }
                };
            }
        });
        viewPager.setCurrentFragment(new Fragment() {
            @Override
            public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                View view = new View(getActivity());
                view.setBackgroundColor(Color.BLUE);
                return view;
            }
        });

        /*this.timelineAdapter = new TimelineAdapter(getSupportFragmentManager(), DateTime.now());
        viewPager.setAdapter(timelineAdapter);

        if (savedInstanceState == null) {
            viewPager.setCurrentItem(timelineAdapter.numberOfDays - 1, false);
        } else {
            timelineAdapter.restoreState(savedInstanceState.getParcelable(SAVED_VIEW_PAGER_STATE), TimelineAdapter.class.getClassLoader());
        }*/
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
