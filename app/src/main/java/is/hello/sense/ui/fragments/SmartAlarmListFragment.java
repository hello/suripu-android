package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.adapter.SmartAlarmAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class SmartAlarmListFragment extends InjectionFragment {
    @Inject SmartAlarmPresenter smartAlarmPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject DateFormatter dateFormatter;

    private ProgressBar loadingIndicator;
    private SmartAlarmAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(smartAlarmPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_smart_alarm_list, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        this.adapter = new SmartAlarmAdapter(getActivity(), dateFormatter);
        listView.setAdapter(adapter);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_smart_alarm_list_progress);

        ImageButton addButton = (ImageButton) view.findViewById(R.id.fragment_smart_alarm_list_add);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Boolean> use24Time = preferences.observableBoolean(PreferencesPresenter.USE_24_TIME, false);
        bindAndSubscribe(use24Time, adapter::setUse24Time, Functions.LOG_ERROR);
        bindAndSubscribe(smartAlarmPresenter.alarms, this::bindAlarms, this::alarmsUnavailable);
    }

    @Override
    public void onResume() {
        super.onResume();

        smartAlarmPresenter.update();
    }


    public void bindAlarms(@NonNull List<SmartAlarm> alarms) {
        adapter.clear();
        adapter.addAll(alarms);

        animate(loadingIndicator)
                .fadeOut(View.GONE)
                .start();
    }

    public void alarmsUnavailable(Throwable e) {
        animate(loadingIndicator)
                .fadeOut(View.GONE)
                .start();

        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
