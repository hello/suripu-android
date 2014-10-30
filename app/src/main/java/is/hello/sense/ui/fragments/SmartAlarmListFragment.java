package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class SmartAlarmListFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final int EDIT_REQUEST_CODE = 0x31;

    @Inject SmartAlarmPresenter smartAlarmPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject DateFormatter dateFormatter;

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
        listView.setOnItemClickListener(this);

        ImageButton addButton = (ImageButton) view.findViewById(R.id.fragment_smart_alarm_list_add);
        addButton.setOnClickListener(this::newAlarm);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LoadingDialogFragment.show(getFragmentManager());
        
        Observable<Boolean> use24Time = preferences.observableBoolean(PreferencesPresenter.USE_24_TIME, false);
        bindAndSubscribe(use24Time, adapter::setUse24Time, Functions.LOG_ERROR);
        bindAndSubscribe(smartAlarmPresenter.alarms, this::bindAlarms, this::alarmsUnavailable);
    }

    @Override
    public void onResume() {
        super.onResume();

        smartAlarmPresenter.update();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {

            } else if (resultCode == SmartAlarmDetailFragment.RESULT_DELETE) {

            }
        }
    }

    public void bindAlarms(@NonNull List<SmartAlarm> alarms) {
        adapter.clear();
        adapter.addAll(alarms);

        LoadingDialogFragment.close(getFragmentManager());
    }

    public void alarmsUnavailable(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    private void editAlarm(@NonNull SmartAlarm alarm) {
        SmartAlarmDetailFragment fragment = SmartAlarmDetailFragment.newInstance(alarm);
        fragment.setTargetFragment(this, EDIT_REQUEST_CODE);
        ((FragmentNavigation) getActivity()).showFragment(fragment, getString(R.string.action_new_alarm), true);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SmartAlarm alarm = (SmartAlarm) adapterView.getItemAtPosition(position);
        editAlarm(alarm);
    }

    public void newAlarm(@NonNull View sender) {
        editAlarm(new SmartAlarm());
    }
}
