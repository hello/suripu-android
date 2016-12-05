package is.hello.sense.ui.fragments.sounds;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.segment.analytics.Properties;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.flows.expansions.utils.ExpansionCategoryFormatter;
import is.hello.sense.flows.smartalarm.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SmartAlarmInteractor;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.SmartAlarmAdapter;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.common.SubFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;
@Deprecated
//todo delete when we use new home activity
public class SmartAlarmListFragment extends SubFragment implements SmartAlarmAdapter.InteractionListener {
    private static final int DELETE_REQUEST_CODE = 0x11;

    @Inject
    SmartAlarmInteractor smartAlarmPresenter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    ExpansionCategoryFormatter expansionCategoryFormatter;

    private RecyclerView recyclerView;
    private ProgressBar activityIndicator;
    private ImageButton addButton;

    private ArrayList<Alarm> currentAlarms = new ArrayList<>();
    private SmartAlarmAdapter adapter;

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Analytics.trackEvent(Analytics.Backside.EVENT_ALARMS, null);
            update();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPresenter(smartAlarmPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.view_smart_alarm_list, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.view_smart_alarm_list_progress);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.view_smart_alarm_list_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        final CardItemDecoration decoration = new CardItemDecoration(resources);
        decoration.contentInset = new Rect(0, 0, 0, resources.getDimensionPixelSize(R.dimen.gap_smart_alarm_list_bottom));
        recyclerView.addItemDecoration(decoration);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));

        this.adapter = new SmartAlarmAdapter(getActivity(),
                                             this,
                                             dateFormatter,
                                             expansionCategoryFormatter);
        recyclerView.setAdapter(adapter);

        this.addButton = (ImageButton) view.findViewById(R.id.view_smart_alarm_list_add);
        Views.setSafeOnClickListener(addButton, this::newAlarm);

        startLoading();

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Observable<Boolean> use24Time = preferences.observableUse24Time();
        bindAndSubscribe(use24Time, adapter::setUse24Time, Functions.LOG_ERROR);
        smartAlarmPresenter.alarms.forget();
        bindAndSubscribe(smartAlarmPresenter.alarms,
                         this::bindAlarms,
                         this::alarmsUnavailable);

    }

    @Override
    public void onResume() {
        super.onResume();
        if(getUserVisibleHint()) {
            update();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.recyclerView = null;
        this.activityIndicator = null;

        this.adapter = null;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DELETE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            final int position = data.getIntExtra(DeleteAlarmDialogFragment.ARG_INDEX, 0);

            startLoading();
            bindAndSubscribe(smartAlarmPresenter.deleteSmartAlarm(position),
                             ignored -> activityIndicator.setVisibility(View.GONE),
                             this::presentError);
        }
    }

    public void startLoading() {
        adapter.clearMessage();
        activityIndicator.setVisibility(View.VISIBLE);
    }

    public void finishLoading() {
        activityIndicator.setVisibility(View.GONE);
    }

    public void bindAlarms(@NonNull final ArrayList<Alarm> alarms) {
        this.currentAlarms = alarms;

        adapter.bindAlarms(alarms);
        if (alarms.isEmpty()) {
            final SmartAlarmAdapter.Message message = new SmartAlarmAdapter.Message(0,
                                                                                    StringRef.from(R.string.message_smart_alarm_placeholder));
            message.actionRes = R.string.action_new_alarm;
            message.titleIconRes = R.drawable.illustration_no_alarm;
            message.onClickListener = this::newAlarm;
            adapter.bindMessage(message);
            addButton.setVisibility(View.GONE);
        } else {
            addButton.setVisibility(View.VISIBLE);
        }

        finishLoading();
    }

    public void alarmsUnavailable(final Throwable e) {
        Logger.error(getClass().getSimpleName(), "Could not load smart alarms.", e);

        final SmartAlarmAdapter.Message message;
        if (ApiException.isNetworkError(e)) {
            message = new SmartAlarmAdapter.Message(0,
                                                    StringRef.from(R.string.error_smart_alarms_unavailable));
            message.actionRes = R.string.action_retry;
            message.onClickListener = this::retry;
        } else if (ApiException.statusEquals(e, 412)) {
            message = new SmartAlarmAdapter.Message(0,
                                                    StringRef.from(R.string.error_smart_alarm_requires_device));
            message.titleIconRes = R.drawable.illustration_no_sense;
            message.actionRes = R.string.action_pair_new_sense;
            message.onClickListener = ignored -> {
                Intent intent = new Intent(getActivity(), OnboardingActivity.class);
                intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SENSE);
                intent.putExtra(OnboardingActivity.EXTRA_PAIR_ONLY, true);
                startActivity(intent);
            };
        } else {
            StringRef errorMessage = Errors.getDisplayMessage(e);
            if (errorMessage == null) {
                errorMessage = StringRef.from(R.string.dialog_error_generic_message);
            }
            message = new SmartAlarmAdapter.Message(0, errorMessage);
            message.actionRes = R.string.action_retry;
            message.onClickListener = this::retry;
        }
        adapter.bindMessage(message);
        addButton.setVisibility(View.GONE);
        finishLoading();
    }

    public void presentError(final Throwable e) {
        finishLoading();

        final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getActivity());
        if (e instanceof SmartAlarmInteractor.DayOverlapError) {
            errorDialogBuilder.withMessage(StringRef.from(R.string.error_smart_alarm_day_overlap));
        }
        final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }


    private void editAlarm(@NonNull final Alarm alarm, final int index) {
        SmartAlarmDetailActivity.startActivity(getActivity(),
                                               alarm,
                                               index);
    }

    @Override
    public void onAlarmClicked(final int position, @NonNull final Alarm alarm) {
        Analytics.trackEvent(Analytics.Backside.EVENT_EDIT_ALARM, null);
        editAlarm(alarm, position);
    }

    @Override
    public boolean onAlarmLongClicked(final int position, @NonNull final Alarm alarm) {
        final DeleteAlarmDialogFragment deleteDialog = DeleteAlarmDialogFragment.newInstance(position);
        deleteDialog.setTargetFragment(this, DELETE_REQUEST_CODE);
        deleteDialog.showAllowingStateLoss(getFragmentManager(), DeleteAlarmDialogFragment.TAG);
        return true;
    }

    @Override
    public void onAlarmEnabledChanged(final int position, final boolean enabled) {
        final Alarm smartAlarm = currentAlarms.get(position);
        smartAlarm.setEnabled(enabled);
        if (enabled && smartAlarm.getDaysOfWeek().isEmpty()) {
            smartAlarm.setRingOnce();
        }

        final String daysRepeated = TextUtils.join(", ", smartAlarm.getSortedDaysOfWeek());
        final Properties properties =
                Analytics.createProperties(Analytics.Backside.PROP_ALARM_ENABLED, smartAlarm.isEnabled(),
                                           Analytics.Backside.PROP_ALARM_IS_SMART, smartAlarm.isSmart(),
                                           Analytics.Backside.PROP_ALARM_DAYS_REPEATED, daysRepeated,
                                           Analytics.Backside.PROP_ALARM_HOUR, smartAlarm.getHourOfDay(),
                                           Analytics.Backside.PROP_ALARM_MINUTE, smartAlarm.getMinuteOfHour());
        Analytics.trackEvent(Analytics.Backside.EVENT_ALARM_ON_OFF, properties);

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

    public void newAlarm(@NonNull final View sender) {
        if (this.currentAlarms.size() >= 30) {
            final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
            dialog.setTitle(R.string.error_to_many_alarms_title);
            dialog.setMessage(R.string.error_to_many_alarms_message);
            dialog.setPositiveButton(R.string.action_ok, null);
            dialog.show();
            return;
        }
        Analytics.trackEvent(Analytics.Backside.EVENT_NEW_ALARM, null);
        editAlarm(new Alarm(), Constants.NONE);
    }

    public void retry(@NonNull final View sender) {
        startLoading();
        update();
    }

    @Override
    public void update() {
        smartAlarmPresenter.update();
    }


    public static class DeleteAlarmDialogFragment extends SenseDialogFragment {
        public static final String ARG_INDEX = DeleteAlarmDialogFragment.class.getName() + ".ARG_ALARM";
        public static final String TAG = DeleteAlarmDialogFragment.class.getSimpleName();

        public static DeleteAlarmDialogFragment newInstance(final int index) {
            final DeleteAlarmDialogFragment fragment = new DeleteAlarmDialogFragment();

            final Bundle arguments = new Bundle();
            arguments.putInt(ARG_INDEX, index);
            fragment.setArguments(arguments);

            return fragment;
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

            dialog.setTitle(R.string.label_delete_alarm);
            dialog.setMessage(R.string.dialog_message_confirm_delete_alarm);
            dialog.setPositiveButton(R.string.action_delete, (sender, which) -> {
                if (getTargetFragment() != null) {
                    final Intent response = new Intent();
                    response.putExtras(getArguments());
                    getTargetFragment().onActivityResult(getTargetRequestCode(),
                                                         Activity.RESULT_OK,
                                                         response);
                }
            });
            dialog.setNegativeButton(android.R.string.cancel, null);
            dialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);

            return dialog;
        }
    }
}
