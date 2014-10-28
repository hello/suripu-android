package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.adapter.SmartAlarmAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

public class SmartAlarmListFragment extends InjectionFragment {
    @Inject SmartAlarmPresenter smartAlarmPresenter;

    private SmartAlarmAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_smart_alarm_list, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        this.adapter = new SmartAlarmAdapter(getActivity());
        listView.setAdapter(adapter);

        ImageButton addButton = (ImageButton) view.findViewById(R.id.fragment_smart_alarm_list_add);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(smartAlarmPresenter.alarms, this::bindAlarms, this::alarmsUnavailable);
    }


    public void bindAlarms(@NonNull List<SmartAlarm> alarms) {
        adapter.clear();
        adapter.addAll(alarms);
    }

    public void alarmsUnavailable(Throwable e) {
        adapter.clear();
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
