package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;

public class TimelineNavigatorFragment extends InjectionFragment {
    public static final String TAG = TimelineNavigatorFragment.class.getSimpleName();

    private static final String ARG_START_DATE = TimelineNavigatorFragment.class.getName() + ".ARG_START_DATE";

    private DateTime startTime;

    private TextView monthText;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;

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

        this.monthText = (TextView) view.findViewById(R.id.fragment_timeline_navigator_month);
        monthText.setText(startTime.toString("MMMM"));


        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_timeline_navigator_recycler_view);

        this.linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, true);
        recyclerView.setLayoutManager(linearLayoutManager);


        Button todayButton = (Button) view.findViewById(R.id.fragment_timeline_navigator_today);
        todayButton.setOnClickListener(this::jumpToToday);

        return view;
    }


    public void jumpToToday(@NonNull View sender) {

    }
}
