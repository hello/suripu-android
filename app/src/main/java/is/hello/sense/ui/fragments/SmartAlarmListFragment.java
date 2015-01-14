package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
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

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.adapter.SmartAlarmAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class SmartAlarmListFragment extends InjectionFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SmartAlarmAdapter.OnAlarmEnabledChanged {
    private static final int EDIT_REQUEST_CODE = 0x31;
    private static final int DELETE_REQUEST_CODE = 0x11;

    @Inject SmartAlarmPresenter smartAlarmPresenter;
    @Inject PreferencesPresenter preferences;

    private ProgressBar activityIndicator;

    private ArrayList<Alarm> currentAlarms = new ArrayList<>();
    private SmartAlarmAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            smartAlarmPresenter.update();
        }

        addPresenter(smartAlarmPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_smart_alarm_list, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_smart_alarm_list_activity);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        this.adapter = new SmartAlarmAdapter(getActivity(), this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        ImageButton addButton = (ImageButton) view.findViewById(R.id.fragment_smart_alarm_list_add);
        Views.setSafeOnClickListener(addButton, this::newAlarm);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Boolean> use24Time = preferences.observableBoolean(PreferencesPresenter.USE_24_TIME, false);
        bindAndSubscribe(use24Time, adapter::setUse24Time, Functions.LOG_ERROR);
        bindAndSubscribe(smartAlarmPresenter.alarms, this::bindAlarms, this::presentError);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        if (requestCode == EDIT_REQUEST_CODE) {
            int index = data.getIntExtra(SmartAlarmDetailActivity.EXTRA_INDEX, 0);
            if (resultCode == Activity.RESULT_OK) {
                Alarm alarm = (Alarm) data.getSerializableExtra(SmartAlarmDetailActivity.EXTRA_ALARM);
                if (index == SmartAlarmDetailActivity.INDEX_NEW) {
                    currentAlarms.add(alarm);
                } else {
                    currentAlarms.set(index, alarm);
                }
            } else if (resultCode == SmartAlarmDetailActivity.RESULT_DELETE) {
                if (index != SmartAlarmDetailActivity.INDEX_NEW) {
                    currentAlarms.remove(index);
                }
            }
        } else if (requestCode == DELETE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            int position = data.getIntExtra(DeleteAlarmDialogFragment.ARG_INDEX, 0);
            currentAlarms.remove(position);
        }

        activityIndicator.setVisibility(View.VISIBLE);
        bindAndSubscribe(smartAlarmPresenter.save(currentAlarms),
                ignored -> activityIndicator.setVisibility(View.GONE),
                this::presentError);
    }

    public void bindAlarms(@NonNull ArrayList<Alarm> alarms) {
        this.currentAlarms = alarms;

        adapter.clear();
        adapter.addAll(alarms);

        activityIndicator.setVisibility(View.GONE);
    }

    public void presentError(Throwable e) {
        activityIndicator.setVisibility(View.GONE);

        if (e instanceof SmartAlarmPresenter.DayOverlapError) {
            ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_smart_alarm_day_overlap));
            dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
        } else {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }
    }


    private void editAlarm(@NonNull Alarm alarm, int index) {
        Bundle arguments = SmartAlarmDetailActivity.getArguments(alarm, index);
        Intent intent = new Intent(getActivity(), SmartAlarmDetailActivity.class);
        intent.putExtras(arguments);
        startActivityForResult(intent, EDIT_REQUEST_CODE);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Analytics.trackEvent(Analytics.EVENT_ALARM_ACTION, Analytics.createProperties(Analytics.PROP_ALARM_ACTION, Analytics.PROP_ALARM_ACTION_EDIT));

        Alarm alarm = (Alarm) adapterView.getItemAtPosition(position);
        editAlarm(alarm, position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        DeleteAlarmDialogFragment deleteDialog = DeleteAlarmDialogFragment.newInstance(position);
        deleteDialog.setTargetFragment(this, DELETE_REQUEST_CODE);
        deleteDialog.show(getFragmentManager(), DeleteAlarmDialogFragment.TAG);
        return true;
    }

    @Override
    public void onAlarmEnabledChanged(int position, boolean enabled) {
        currentAlarms.get(position).setEnabled(enabled);

        if (!enabled) {
            Analytics.trackEvent(Analytics.EVENT_ALARM_ACTION, Analytics.createProperties(Analytics.PROP_ALARM_ACTION, Analytics.PROP_ALARM_ACTION_DISABLE));
        }

        activityIndicator.setVisibility(View.VISIBLE);
        bindAndSubscribe(smartAlarmPresenter.save(currentAlarms),
                ignored -> activityIndicator.setVisibility(View.GONE),
                this::presentError);
    }

    public void newAlarm(@NonNull View sender) {
        Analytics.trackEvent(Analytics.EVENT_ALARM_ACTION, Analytics.createProperties(Analytics.PROP_ALARM_ACTION, Analytics.PROP_ALARM_ACTION_ADD));
        editAlarm(new Alarm(), SmartAlarmDetailActivity.INDEX_NEW);
    }


    public static class DeleteAlarmDialogFragment extends DialogFragment {
        public static final String ARG_INDEX = DeleteAlarmDialogFragment.class.getName() + ".ARG_ALARM";
        public static final String TAG = DeleteAlarmDialogFragment.class.getSimpleName();

        public static DeleteAlarmDialogFragment newInstance(int index) {
            DeleteAlarmDialogFragment fragment = new DeleteAlarmDialogFragment();

            Bundle arguments = new Bundle();
            arguments.putInt(ARG_INDEX, index);
            fragment.setArguments(arguments);

            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

            dialog.setTitle(R.string.label_delete_alarm);
            dialog.setMessage(R.string.dialog_message_confirm_delete_alarm);
            dialog.setPositiveButton(R.string.action_delete, (sender, which) -> {
                if (getTargetFragment() != null) {
                    Intent response = new Intent();
                    response.putExtras(getArguments());
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
                }
            });
            dialog.setNegativeButton(android.R.string.cancel, null);
            dialog.setDestructive(true);

            return dialog;
        }
    }
}
