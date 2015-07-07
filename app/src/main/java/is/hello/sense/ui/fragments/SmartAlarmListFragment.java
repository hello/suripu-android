package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
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

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.adapter.SmartAlarmAdapter;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
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

        this.listView = (ListView) view.findViewById(android.R.id.list);
        this.adapter = new SmartAlarmAdapter(getActivity(), this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        View spacer = new View(getActivity());
        spacer.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.gap_smart_alarm_list_bottom));
        ListViews.addFooterView(listView, spacer, null, false);

        Styles.addCardSpacing(listView, Styles.CARD_SPACING_HEADER | Styles.CARD_SPACING_USE_COMPACT);

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

        this.listView = null;
        this.activityIndicator = null;
        this.emptyPrompt = null;

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
        adapter.clearMessage();
        emptyPrompt.setVisibility(View.GONE);
        activityIndicator.setVisibility(View.VISIBLE);
    }

    public void finishLoading(boolean success) {
        activityIndicator.setVisibility(View.GONE);

        if (currentAlarms.size() == 0) {
            if (success) {
                emptyPrompt.setVisibility(View.VISIBLE);
            } else {
                emptyPrompt.setVisibility(View.GONE);
            }
        } else {
            emptyPrompt.setVisibility(View.GONE);
        }
    }

    public void bindAlarms(@NonNull ArrayList<Alarm> alarms) {
        this.currentAlarms = alarms;

        adapter.bindAlarms(alarms);
        if (alarms.isEmpty()) {
            SmartAlarmAdapter.Message message = new SmartAlarmAdapter.Message(R.string.title_smart_alarms,
                    StringRef.from(R.string.message_smart_alarm_placeholder));
            message.actionRes = R.string.action_new_alarm;
            message.onClickListener = this::newAlarm;
            adapter.bindMessage(message);
        }

        finishLoading(true);
    }

    public void alarmsUnavailable(Throwable e) {
        Logger.error(getClass().getSimpleName(), "Could not load smart alarms.", e);

        SmartAlarmAdapter.Message message;
        if (ApiException.isNetworkError(e)) {
            message = new SmartAlarmAdapter.Message(R.string.dialog_error_title,
                    StringRef.from(R.string.error_network_unavailable));
            message.actionRes = R.string.action_retry;
            message.onClickListener = this::retry;
        } else if (ApiException.statusEquals(e, 400)) {
            message = new SmartAlarmAdapter.Message(R.string.dialog_error_title,
                    StringRef.from(R.string.error_smart_alarm_clock_drift));
            message.actionRes = R.string.action_retry;
            message.onClickListener = this::retry;
        } else if (ApiException.statusEquals(e, 412)) {
            message = new SmartAlarmAdapter.Message(R.string.device_sense,
                    StringRef.from(R.string.error_smart_alarm_requires_device));
            message.titleIconRes = R.drawable.sense_icon;
            message.titleStyleRes = R.style.AppTheme_Text_Body;
            message.actionRes = R.string.action_pair_new_sense;
            message.onClickListener = ignored -> {
                DeviceListFragment.startStandaloneFrom(getActivity());
            };
        } else {
            message = new SmartAlarmAdapter.Message(R.string.dialog_error_title,
                    StringRef.from(e.getMessage()));
            message.actionRes = R.string.action_retry;
            message.onClickListener = this::retry;
        }
        adapter.bindMessage(message);

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
        if (adapter.isShowingMessage()) {
            return;
        }

        Analytics.trackEvent(Analytics.TopView.EVENT_EDIT_ALARM, null);

        Alarm alarm = (Alarm) adapterView.getItemAtPosition(position);
        int alarmPosition = ListViews.getAdapterPosition(listView, position);
        editAlarm(alarm, alarmPosition);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (adapter.isShowingMessage()) {
            return false;
        }

        int alarmPosition = ListViews.getAdapterPosition(listView, position);
        DeleteAlarmDialogFragment deleteDialog = DeleteAlarmDialogFragment.newInstance(alarmPosition);
        deleteDialog.setTargetFragment(this, DELETE_REQUEST_CODE);
        deleteDialog.showAllowingStateLoss(getFragmentManager(), DeleteAlarmDialogFragment.TAG);
        return true;
    }

    @Override
    public void onAlarmEnabledChanged(int position, boolean enabled) {
        Alarm smartAlarm = currentAlarms.get(position);
        smartAlarm.setEnabled(enabled);
        if (enabled && smartAlarm.getDaysOfWeek().isEmpty()) {
            smartAlarm.setRingOnce();
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

    public void retry(@NonNull View sender) {
        startLoading();
        onUpdate();
    }


    public static class DeleteAlarmDialogFragment extends SenseDialogFragment {
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
