package is.hello.sense.ui.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class SensorHistoryFragment extends InjectionFragment {
    @Inject CurrentConditionsPresenter conditionsPresenter;
    @Inject UnitFormatter unitsFormatter;

    private TextView readingText;
    private TextView messageText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(conditionsPresenter);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor_history, container, false);

        this.readingText = (TextView) view.findViewById(R.id.fragment_sensor_history_reading);
        this.messageText = (TextView) view.findViewById(R.id.fragment_sensor_history_message);

        ViewPager viewPager = (ViewPager) view.findViewById(R.id.fragment_sensor_history_view_pager);
        viewPager.setAdapter(new FragmentAdapter(getChildFragmentManager()));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Pair<RoomConditions, UnitSystem>> currentConditions = Observable.combineLatest(conditionsPresenter.currentConditions, unitsFormatter.unitSystem, Pair::new);
        track(bindFragment(this, currentConditions).subscribe(this::bindConditions, this::presentError));
    }


    public SensorHistoryActivity getSensorHistoryActivity() {
        return (SensorHistoryActivity) getActivity();
    }


    public void bindConditions(@NonNull Pair<RoomConditions, UnitSystem> pair) {
        RoomConditions conditions = pair.first;
        UnitSystem unitSystem = pair.second;

        SensorState condition = conditions.getSensorStateWithName(getSensorHistoryActivity().getSensor());
        if (condition != null) {
            UnitFormatter.Formatter formatter = SensorHistory.SENSOR_NAME_TEMPERATURE.equals(getSensorHistoryActivity().getSensor()) ? unitSystem::formatTemperature : null;
            String formattedValue = condition.getFormattedValue(formatter);
            if (formattedValue != null)
                readingText.setText(formattedValue);
            else
                readingText.setText(R.string.missing_data_placeholder);

            messageText.setText(condition.getMessage());
        }
    }

    public void presentError(@NonNull Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    private class FragmentAdapter extends FragmentPagerAdapter {
        private FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "24 Hours";

                case 1:
                    return "Last Week";

                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public Fragment getItem(int position) {
            int mode;
            switch (position) {
                case 0:
                    mode = SensorHistoryPresenter.MODE_DAY;
                    break;

                case 1:
                    mode = SensorHistoryPresenter.MODE_WEEK;
                    break;

                default:
                    throw new IllegalArgumentException();
            }

            return SensorHistoryGraphFragment.newInstance(getSensorHistoryActivity().getSensor(), mode);
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
