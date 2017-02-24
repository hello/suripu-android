package is.hello.sense.flows.home.ui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.segment.analytics.Properties;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.flows.expansions.utils.ExpansionCategoryFormatter;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.views.SmartAlarmListView;
import is.hello.sense.flows.smartalarm.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SmartAlarmInteractor;
import is.hello.sense.mvp.presenters.ControllerPresenterFragment;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.SmartAlarmAdapter;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.NotTested;
import rx.Observable;

@NotTested
public class SmartAlarmListFragment extends ControllerPresenterFragment<SmartAlarmListView>
        implements
        SmartAlarmAdapter.InteractionListener,
        HomeActivity.ScrollUp,
        ViewPagerPresenterFragment.FabListener {
    private static final int DELETE_REQUEST_CODE = 117;
    private static final int PAIR_SENSE_REQUEST_CODE = 118;

    @Inject
    SmartAlarmInteractor smartAlarmInteractor;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    ExpansionCategoryFormatter expansionCategoryFormatter;
    private ArrayList<Alarm> currentAlarms = new ArrayList<>();
    @Nullable
    private ViewPagerPresenterFragment.NotificationListener notificationListener = null;


    //region PresenterFragment
    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            this.presenterView = new SmartAlarmListView(getActivity(),
                                                        new SmartAlarmAdapter(getActivity(),
                                                                              this,
                                                                              dateFormatter,
                                                                              expansionCategoryFormatter));
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(smartAlarmInteractor);
        addInteractor(preferences);
        addInteractor(dateFormatter);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Observable<Boolean> use24Time = preferences.observableUse24Time();
        bindAndSubscribe(use24Time, presenterView::updateAdapterTime, Functions.LOG_ERROR);
        smartAlarmInteractor.alarms.forget();
        bindAndSubscribe(smartAlarmInteractor.alarms,
                         this::bindAlarms,
                         this::alarmsUnavailable);
        presenterView.setProgressBarVisible(true);
        smartAlarmInteractor.update();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.notificationListener = null;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DELETE_REQUEST_CODE &&
                resultCode == Activity.RESULT_OK &&
                data != null) {
            final int position = data.getIntExtra(DeleteAlarmDialogFragment.ARG_INDEX, 0);

            presenterView.setProgressBarVisible(true);
            bindAndSubscribe(smartAlarmInteractor.deleteSmartAlarm(position),
                             ignored -> presenterView.setProgressBarVisible(false),
                             this::presentError);
        } else if (requestCode == PAIR_SENSE_REQUEST_CODE) {
            this.setVisibleToUser(true);
        }
    }

    @Override
    public void setVisibleToUser(final boolean isVisible) {
        super.setVisibleToUser(isVisible);
        if (isVisible) {
            Analytics.trackEvent(Analytics.Backside.EVENT_ALARMS, null);
            updateAlarms(null);
        }
    }
    //endregion

    //region InteractionListener
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

        presenterView.setProgressBarVisible(true);
        bindAndSubscribe(smartAlarmInteractor.saveSmartAlarm(position, smartAlarm),
                         ignored -> presenterView.setProgressBarVisible(false),
                         e -> {
                             // Revert on error
                             smartAlarm.setEnabled(!enabled);
                             presenterView.notifyAdapterUpdate();

                             presentError(e);
                         });
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
    //endregion

    //region ScrollUp
    @Override
    public void scrollUp() {
        if (presenterView == null) {
            return;
        }
        presenterView.scrollUp();
    }
    //endregion

    //region FabListener
    @Override
    public void onFabClick() {
        this.onAddButtonClicked(null);
    }

    @Override
    public boolean shouldShowFab() {
        return smartAlarmInteractor != null
                && smartAlarmInteractor.alarms != null
                && smartAlarmInteractor.alarms.hasValue()
                && !smartAlarmInteractor.alarms.getValue().isEmpty();
    }

    @DrawableRes
    @Override
    public int getFabDrawableRes() {
        return R.drawable.icon_plus;
    }

    @Override
    public void setNotificationListener(@NonNull final ViewPagerPresenterFragment.NotificationListener notificationListener) {
        this.notificationListener = notificationListener;
    }

    @Override
    public boolean hasNotificationListener() {
        return notificationListener != null;
    }

    @Override
    public boolean shouldFabRotate() {
        return false;
    }
    //endregion

    //region methods
    private void notifyChange() {
        if (this.notificationListener != null) {
            this.notificationListener.notifyChange();
        }
    }

    public void onAddButtonClicked(@Nullable final View ignored) {
        if (this.currentAlarms.size() >= 30) {
            showAlertDialog(new SenseAlertDialog.Builder()
                                    .setTitle(R.string.error_to_many_alarms_title)
                                    .setMessage(R.string.error_to_many_alarms_message)
                                    .setPositiveButton(R.string.action_ok, null));
            return;
        }
        Analytics.trackEvent(Analytics.Backside.EVENT_NEW_ALARM, null);
        final Alarm newAlarm = new Alarm();
        newAlarm.setSmart(smartAlarmInteractor.canBeSmartWith(newAlarm,
                                                              currentAlarms));
        editAlarm(newAlarm, Constants.NONE);
    }

    private void editAlarm(@NonNull final Alarm alarm, final int index) {
        SmartAlarmDetailActivity.startActivity(getActivity(),
                                               alarm,
                                               index);
    }


    public void bindAlarms(@NonNull final ArrayList<Alarm> alarms) {
        this.currentAlarms = alarms;

        presenterView.updateAdapterAlarms(alarms);
        if (alarms.isEmpty()) {
            final SmartAlarmAdapter.Message message = new SmartAlarmAdapter.Message(0,
                                                                                    StringRef.from(R.string.message_smart_alarm_placeholder));
            message.actionRes = R.string.action_new_alarm;
            message.titleIconRes = R.drawable.illustration_no_alarm;
            message.onClickListener = this::onAddButtonClicked;
            presenterView.bindAdapterMessage(message);
        }
        notifyChange();
        presenterView.setProgressBarVisible(false);
    }

    public void alarmsUnavailable(final Throwable e) {
        Logger.error(getClass().getSimpleName(), "Could not load smart alarms.", e);

        final SmartAlarmAdapter.Message message;
        if (ApiException.isNetworkError(e)) {
            message = new SmartAlarmAdapter.Message(0,
                                                    StringRef.from(R.string.error_smart_alarms_unavailable));
            message.actionRes = R.string.action_retry;
            message.onClickListener = this::updateAlarms;
        } else if (ApiException.statusEquals(e, 412)) {
            message = new SmartAlarmAdapter.Message(0,
                                                    StringRef.from(R.string.error_smart_alarm_requires_device));
            message.titleIconRes = R.drawable.illustration_no_sense;
            message.actionRes = R.string.action_pair_sense;
            message.onClickListener = ignored -> SmartAlarmListFragment.this.startPairSense();
        } else {
            StringRef errorMessage = Errors.getDisplayMessage(e);
            if (errorMessage == null) {
                errorMessage = StringRef.from(R.string.dialog_error_generic_message);
            }
            message = new SmartAlarmAdapter.Message(0, errorMessage);
            message.actionRes = R.string.action_retry;
            message.onClickListener = this::updateAlarms;
        }
        notifyChange();
        presenterView.bindAdapterMessage(message);
        presenterView.setProgressBarVisible(false);
    }

    public void updateAlarms(@Nullable final View ignored) {
        smartAlarmInteractor.update();
    }

    public void startPairSense() {
        startActivityForResult(OnboardingActivity.getPairOnlyIntent(getActivity()),
                               PAIR_SENSE_REQUEST_CODE);
    }

    public void presentError(final Throwable e) {
        presenterView.setProgressBarVisible(false);

        final ErrorDialogFragment.PresenterBuilder builder = new ErrorDialogFragment.PresenterBuilder(e);
        if (e instanceof SmartAlarmInteractor.DayOverlapError) {
            builder.withMessage(StringRef.from(R.string.error_smart_alarm_day_overlap));
        }
        showErrorDialog(builder);
    }

    //endregion


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
