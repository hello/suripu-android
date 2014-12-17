package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;

public class TimelineNavigatorFragment extends InjectionFragment {
    public static final String TAG = TimelineNavigatorFragment.class.getSimpleName();

    private static final String ARG_START_DATE = TimelineNavigatorFragment.class.getName() + ".ARG_START_DATE";

    private DateTime startTime;

    public static TimelineNavigatorFragment newInstance(@NonNull DateTime startTime) {
        TimelineNavigatorFragment fragment = new TimelineNavigatorFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_START_DATE, startTime);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.startTime = (DateTime) getArguments().getSerializable(ARG_START_DATE);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline_navigator, container, false);

        return view;
    }
}
