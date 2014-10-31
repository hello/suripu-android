package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
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

public class SmartAlarmListFragment extends InjectionFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private static final int EDIT_REQUEST_CODE = 0x31;
    private static final int DELETE_REQUEST_CODE = 0x11;

    @Inject SmartAlarmPresenter smartAlarmPresenter;
    @Inject PreferencesPresenter preferences;
    @Inject DateFormatter dateFormatter;

    private List<SmartAlarm> currentAlarms = new ArrayList<>();
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
        listView.setOnItemLongClickListener(this);

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
            int index = data.getIntExtra(SmartAlarmDetailFragment.ARG_INDEX, 0);
            if (resultCode == Activity.RESULT_OK) {
                SmartAlarm alarm = (SmartAlarm) data.getSerializableExtra(SmartAlarmDetailFragment.ARG_ALARM);
                if (index == SmartAlarmDetailFragment.INDEX_NEW) {
                    currentAlarms.add(alarm);
                } else {
                    currentAlarms.set(index, alarm);
                }
            } else if (resultCode == SmartAlarmDetailFragment.RESULT_DELETE) {
                if (index != SmartAlarmDetailFragment.INDEX_NEW) {
                    currentAlarms.remove(index);
                }
            }

            LoadingDialogFragment.show(getFragmentManager());
            smartAlarmPresenter.save(currentAlarms);
        } else if (requestCode == DELETE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            int position = data.getIntExtra(DeleteAlarmDialogFragment.ARG_INDEX, 0);
            currentAlarms.remove(position);

            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(smartAlarmPresenter.save(currentAlarms),
                             ignored -> LoadingDialogFragment.close(getFragmentManager()),
                             this::alarmsUnavailable);
        }
    }

    public void bindAlarms(@NonNull List<SmartAlarm> alarms) {
        this.currentAlarms = alarms;

        adapter.clear();
        adapter.addAll(alarms);

        LoadingDialogFragment.close(getFragmentManager());
    }

    public void alarmsUnavailable(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    private void editAlarm(@NonNull SmartAlarm alarm, int index) {
        SmartAlarmDetailFragment fragment = SmartAlarmDetailFragment.newInstance(alarm, index);
        fragment.setTargetFragment(this, EDIT_REQUEST_CODE);
        ((FragmentNavigation) getActivity()).showFragment(fragment, getString(R.string.action_new_alarm), true);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SmartAlarm alarm = (SmartAlarm) adapterView.getItemAtPosition(position);
        editAlarm(alarm, position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        DeleteAlarmDialogFragment deleteDialog = DeleteAlarmDialogFragment.newInstance(position);
        deleteDialog.setTargetFragment(this, DELETE_REQUEST_CODE);
        deleteDialog.show(getFragmentManager(), DeleteAlarmDialogFragment.TAG);
        return true;
    }

    public void newAlarm(@NonNull View sender) {
        editAlarm(new SmartAlarm(), SmartAlarmDetailFragment.INDEX_NEW);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.label_delete_alarm);
            builder.setMessage(R.string.dialog_message_confirm_delete_alarm);
            builder.setPositiveButton(R.string.action_delete, (dialog, which) -> {
                if (getTargetFragment() != null) {
                    Intent response = new Intent();
                    response.putExtras(getArguments());
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }
}
