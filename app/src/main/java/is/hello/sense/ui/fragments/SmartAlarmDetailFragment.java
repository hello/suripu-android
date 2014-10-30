package is.hello.sense.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.api.model.SmartAlarm;

public class SmartAlarmDetailFragment extends Fragment {
    private static final String ARG_ALARM = SmartAlarmDetailFragment.class.getName() + ".ARG_ALARM";

    private SmartAlarm smartAlarm;

    public static @NonNull SmartAlarmDetailFragment newInstance(@NonNull SmartAlarm smartAlarm) {
        SmartAlarmDetailFragment detailFragment = new SmartAlarmDetailFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_ALARM, smartAlarm);
        detailFragment.setArguments(arguments);

        return detailFragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.smartAlarm = (SmartAlarm) getArguments().getSerializable(ARG_ALARM);

        setRetainInstance(true);
    }
}
