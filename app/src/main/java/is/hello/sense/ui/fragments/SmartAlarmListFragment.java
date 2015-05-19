package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.adapter.SmartAlarmAdapter;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;
import rx.Observable;

public class SmartAlarmListFragment extends UndersideTabFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SmartAlarmAdapter.OnAlarmEnabledChanged {
    private static final int DELETE_REQUEST_CODE = 0x11;

    @Inject SmartAlarmPresenter smartAlarmPresenter;
    @Inject PreferencesPresenter preferences;

    private ListView listView;
    private ProgressBar activityIndicator;
    private View emptyPrompt;
    private View emptyView;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private Button emptyRetry;

    private ArrayList<Alarm> currentAlarms = new ArrayList<>();
    private SmartAlarmAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_ALARMS, null);
        }

        addPresenter(smartAlarmPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_smart_alarm_list, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_smart_alarm_list_activity);

        this.emptyPrompt = view.findViewById(R.id.fragment_smart_alarm_list_first_prompt);
        this.emptyView = view.findViewById(android.R.id.empty);
        this.emptyTitle = (TextView) emptyView.findViewById(R.id.fragment_smart_alarm_empty_title);
        this.emptyMessage = (TextView) emptyView.findViewById(R.id.fragment_smart_alarm_empty_message);
        this.emptyRetry = (Button) emptyView.findViewById(R.id.fragment_smart_alarm_empty_retry);
        Views.setSafeOnClickListener(emptyRetry, ignored -> {
            startLoading();
            smartAlarmPresenter.update();
        });

        this.listView = (ListView) view.findViewById(android.R.id.list);
        this.adapter = new SmartAlarmAdapter(getActivity(), this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        View spacer = new View(getActivity());
        spacer.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.gap_smart_alarm_list_bottom));
        ListViews.addFooterView(listView, spacer, null, false);

        Styles.addCardSpacing(listView, Styles.CARD_SPACING_HEADER | Styles.CARD_SPACING_USE_COMPACT, null);

        ImageButton addButton = (ImageButton) view.findViewById(R.id.fragment_smart_alarm_list_add);
        Views.setSafeOnClickListener(addButton, this::newAlarm);

        startLoading();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Boolean> use24Time = preferences.observableUse24Time();
        bindAndSubscribe(use24Time, adapter::setUse24Time, Functions.LOG_ERROR);
        bindAndSubscribe(smartAlarmPresenter.alarms, this::bindAlarms, this::alarmsUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.adapter = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DELETE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            int position = data.getIntExtra(DeleteAlarmDialogFragment.ARG_INDEX, 0);

            startLoading();
            bindAndSubscribe(smartAlarmPresenter.deleteSmartAlarm(position),
                    ignored -> activityIndicator.setVisibility(View.GONE),
                    this::presentError);
        }
    }

    @Override
    public void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {
        smartAlarmPresenter.update();
    }

    public void startLoading() {
        emptyView.setVisibility(View.GONE);
        emptyPrompt.setVisibility(View.GONE);
        activityIndicator.setVisibility(View.VISIBLE);
    }

    public void finishLoading(boolean success) {
        activityIndicator.setVisibility(View.GONE);

        if (adapter.getCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            if (success) {
                emptyPrompt.setVisibility(View.VISIBLE);
            } else {
                emptyPrompt.setVisibility(View.GONE);
            }
        } else {
            emptyView.setVisibility(View.GONE);
            emptyPrompt.setVisibility(View.GONE);
        }
    }

    public void bindAlarms(@NonNull ArrayList<Alarm> alarms) {
        this.currentAlarms = alarms;

        adapter.clear();
        adapter.addAll(alarms);

        emptyTitle.setText(R.string.title_smart_alarms);
        emptyMessage.setText(R.string.message_smart_alarm_placeholder);
        emptyRetry.setVisibility(View.GONE);

        finishLoading(true);
    }

    public void alarmsUnavailable(Throwable e) {
        Logger.error(getClass().getSimpleName(), "Could not load smart alarms.", e);
        adapter.clear();

        emptyTitle.setText(R.string.dialog_error_title);
        if (ApiException.isNetworkError(e)) {
            emptyMessage.setText(R.string.error_network_unavailable);
        } else if (ApiException.statusEquals(e, 400)) {
            emptyMessage.setText(R.string.error_smart_alarm_clock_drift);
        } else if (ApiException.statusEquals(e, 412)) {
            emptyMessage.setText(R.string.error_smart_alarm_requires_device);
        } else {
            emptyMessage.setText(e.getMessage());
        }
        emptyRetry.setVisibility(View.VISIBLE);

        finishLoading(false);
    }

    public void presentError(Throwable e) {
        finishLoading(true);

        if (e instanceof SmartAlarmPresenter.DayOverlapError) {
            ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_smart_alarm_day_overlap));
            dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
        } else if (ApiException.statusEquals(e, 400)) {
            ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_smart_alarm_clock_drift));
            dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
        } else {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }
    }


    private void editAlarm(@NonNull Alarm alarm, int index) {
        Bundle arguments = SmartAlarmDetailActivity.getArguments(alarm, index);
        Intent intent = new Intent(getActivity(), SmartAlarmDetailActivity.class);
        intent.putExtras(arguments);
        startActivity(intent);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Analytics.trackEvent(Analytics.TopView.EVENT_EDIT_ALARM, null);

        Alarm alarm = (Alarm) adapterView.getItemAtPosition(position);
        int alarmPosition = ListViews.getAdapterPosition(listView, position);
        editAlarm(alarm, alarmPosition);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        int alarmPosition = ListViews.getAdapterPosition(listView, position);
        DeleteAlarmDialogFragment deleteDialog = DeleteAlarmDialogFragment.newInstance(alarmPosition);
        deleteDialog.setTargetFragment(this, DELETE_REQUEST_CODE);
        deleteDialog.show(getFragmentManager(), DeleteAlarmDialogFragment.TAG);
        return true;
    }

    @Override
    public void onAlarmEnabledChanged(int position, boolean enabled) {
        Alarm smartAlarm = currentAlarms.get(position);
        smartAlarm.setEnabled(enabled);
        if (enabled && smartAlarm.getDaysOfWeek().isEmpty()) {
            smartAlarm.fireOnce();
        }

        Analytics.trackEvent(Analytics.TopView.EVENT_ALARM_ON_OFF, null);

        activityIndicator.setVisibility(View.VISIBLE);
        bindAndSubscribe(smartAlarmPresenter.saveSmartAlarm(position, smartAlarm),
                ignored -> activityIndicator.setVisibility(View.GONE),
                e -> {
                    // Revert on error
                    smartAlarm.setEnabled(!enabled);
                    adapter.notifyDataSetChanged();

                    presentError(e);
                });
    }

    public void newAlarm(@NonNull View sender) {
        Analytics.trackEvent(Analytics.TopView.EVENT_NEW_ALARM, null);
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
            dialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);

            return dialog;
        }
    }
}
