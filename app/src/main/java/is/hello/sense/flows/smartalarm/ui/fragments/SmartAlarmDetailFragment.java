package is.hello.sense.flows.smartalarm.ui.fragments;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import com.segment.analytics.Properties;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.ExpansionAlarm;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.flows.expansions.interactors.ExpansionsInteractor;
import is.hello.sense.flows.expansions.utils.ExpansionCategoryFormatter;
import is.hello.sense.flows.smartalarm.ui.views.SmartAlarmDetailView;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.HasVoiceInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SmartAlarmInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.TimePickerDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.GenericListObject;
import is.hello.sense.util.NotTested;
import rx.Observable;

public class SmartAlarmDetailFragment extends PresenterFragment<SmartAlarmDetailView>
        implements OnBackPressedInterceptor {
    //region static functions and fields
    public static final String ARG_ALARM = SmartAlarmDetailFragment.class.getName() + ".ARG_ALARM";
    public static final String ARG_INDEX = SmartAlarmDetailFragment.class.getName() + ".ARG_INDEX";
    public static final String ARG_SKIP = SmartAlarmDetailFragment.class.getName() + ".ARG_SKIP";
    public static final String KEY_ALARM = SmartAlarmDetailFragment.class.getName() + ".KEY_ALARM";
    public static final String KEY_INDEX = SmartAlarmDetailFragment.class.getName() + ".KEY_INDEX";
    public static final String KEY_SKIP = SmartAlarmDetailFragment.class.getName() + ".KEY_SKIP";
    public static final String KEY_DIRTY = SmartAlarmDetailFragment.class.getName() + ".KEY_DIRTY";
    public static final String EXTRA_EXPANSION_ID = SmartAlarmDetailFragment.class.getName() + ".EXTRA_EXPANSION_ID";
    public static final String EXTRA_EXPANSION = SmartAlarmDetailFragment.class.getName() + ".EXTRA_EXPANSION";
    public static final String EXTRA_EXPANSION_ALARM = SmartAlarmDetailFragment.class.getName() + ".EXTRA_EXPANSION_ALARM";
    public static final int RESULT_AUTHENTICATE_EXPANSION = 3101;
    public static final int RESULT_PICKER_EXPANSION = 3102;
    public static final int RESULT_PICKER_EXPANSION_ALARM = 3103;
    private static final int TIME_REQUEST_CODE = 1863;
    private static final int SOUND_REQUEST_CODE = 80;
    private static final int REPEAT_REQUEST_CODE = 89;
    private static final int EXPANSION_VALUE_REQUEST_CODE = 105;
    private static final int DEFAULT_HOUR = 7;
    private static final int DEFAULT_MINUTE = 30;


    public static SmartAlarmDetailFragment newInstance(@NonNull final Alarm alarm,
                                                       final int index,
                                                       final boolean skipUI) {
        final Bundle args = new Bundle();
        args.putSerializable(ARG_ALARM, alarm);
        args.putInt(ARG_INDEX, index);
        args.putBoolean(ARG_SKIP, skipUI);
        final SmartAlarmDetailFragment fragment = new SmartAlarmDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }
    //endregion

    //region class fields
    @Inject
    DateFormatter dateFormatter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    HasVoiceInteractor hasVoiceInteractor;
    @Inject
    SmartAlarmInteractor smartAlarmInteractor;
    @Inject
    ExpansionsInteractor expansionsInteractor;
    @Inject
    ExpansionCategoryFormatter expansionCategoryFormatter;

    private boolean dirty;
    private Alarm alarm;
    private int index;
    private boolean skipUI = false;
    private boolean use24Time = false;
    private boolean wantsTone = false;
    //endregion

    //region PresenterFragment
    @NotTested
    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new SmartAlarmDetailView(getActivity(),
                                                          this::onTimeClicked,
                                                          this::onHelpClicked,
                                                          this::onToneClicked,
                                                          this::onRepeatClicked);
        }
    }

    @NotTested
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putBoolean(KEY_SKIP, this.skipUI); // probably won't ever be needed.
        outState.putSerializable(KEY_ALARM, this.alarm);
        outState.putInt(KEY_INDEX, this.index);
        outState.putBoolean(KEY_DIRTY, this.dirty);
        super.onSaveInstanceState(outState);
    }


    @NotTested
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addInteractor(this.smartAlarmInteractor);
        addInteractor(this.hasVoiceInteractor);
        addInteractor(this.preferences);
        addInteractor(this.expansionsInteractor);
    }

    @NotTested
    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            this.dirty = savedInstanceState.getBoolean(KEY_DIRTY, false);
            this.alarm = (Alarm) savedInstanceState.getSerializable(KEY_ALARM);
            this.skipUI = savedInstanceState.getBoolean(KEY_SKIP);
            this.index = savedInstanceState.getInt(KEY_INDEX);
        } else {
            final Bundle args = getArguments();
            if (args == null) {
                // should never happen but just in case.
                cancelFlow();
                return;
            }
            this.skipUI = args.getBoolean(ARG_SKIP, false);
            this.alarm = (Alarm) args.getSerializable(ARG_ALARM);
            this.index = args.getInt(ARG_INDEX);
            this.dirty = index == Constants.NONE;
            this.expansionsInteractor.update();
            if (alarm == null) {
                // should never happen but just in case.
                cancelFlow();
                return;
            }
        }
        updateUIForAlarm();
        bindAndSubscribe(this.expansionsInteractor.expansions,
                         this::bindExpansions,
                         this::bindExpansionError);
        bindAndSubscribe(this.hasVoiceInteractor.hasVoice,
                         hasVoice -> {
                             if (hasVoice) {
                                 this.presenterView.showExpansionsContainer();
                             }
                         },
                         Functions.LOG_ERROR);

        bindAndSubscribe(this.preferences.observableUse24Time(),
                         newValue -> {
                             this.use24Time = newValue;
                             updateUIForAlarm();
                         },
                         Functions.LOG_ERROR);

        bindAndSubscribe(this.smartAlarmInteractor.availableAlarmSounds(),
                         this::bindSmartAlarmSounds,
                         Functions.LOG_ERROR);
        this.smartAlarmInteractor.update();
        this.hasVoiceInteractor.update();
    }

    @NotTested
    @Override
    public void onResume() {
        super.onResume();
        if (this.skipUI) {
            LoadingDialogFragment.show(getFragmentManager(),
                                       null, LoadingDialogFragment.DEFAULTS);
        } else {
            WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_alarm, false);
        }
    }

    @NotTested
    @Override
    public void onActivityResult(final int requestCode,
                                 final int resultCode,
                                 final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }

        if (requestCode == TIME_REQUEST_CODE) {
            final int hour = data.getIntExtra(TimePickerDialogFragment.RESULT_HOUR, DEFAULT_HOUR);
            final int minute = data.getIntExtra(TimePickerDialogFragment.RESULT_MINUTE, DEFAULT_MINUTE);
            this.alarm.setTime(new LocalTime(hour, minute));
            updateUIForAlarm();
            markDirty();
        } else if (requestCode == SOUND_REQUEST_CODE) {
            final int soundId = data.getIntExtra(ListActivity.VALUE_ID, -1);
            if (soundId != -1) {
                final Alarm.Sound selectedSound = this.alarm.getAlarmSoundWithId(soundId);
                if (selectedSound != null) {
                    this.alarm.setSound(selectedSound);
                    this.presenterView.setTone(selectedSound.name);
                }
            }
            markDirty();
        } else if (requestCode == REPEAT_REQUEST_CODE) {
            final List<Integer> selectedDays = data.getIntegerArrayListExtra(ListActivity.VALUE_ID);
            this.alarm.setDaysOfWeek(selectedDays);
            this.presenterView.setRepeatDaysTextView(this.alarm.getRepeatSummary(getActivity(), false));
            markDirty();
        }
    }

    @NotTested
    @Override
    public void onCreateOptionsMenu(final Menu menu,
                                    final MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.alarm_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @NotTested
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_save:
                saveAlarm();
                return true;
        }
        return false;
    }

    //endregion

    //region OnBackPressedInterceptor

    @NotTested
    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if (this.dirty) {
            final SenseAlertDialog.Builder builder = new SenseAlertDialog.Builder();
            if (isNewAlarm()) {
                builder.setTitle(R.string.dialog_title_smart_alarm_new_cancel);
                builder.setMessage(R.string.dialog_message_smart_alarm_new_cancel);
            } else {
                builder.setTitle(R.string.dialog_title_smart_alarm_edit_cancel);
                builder.setMessage(R.string.dialog_message_smart_alarm_edit_cancel);
            }
            builder.setNegativeButton(R.string.action_keep_editing, null);
            builder.setPositiveButton(R.string.action_discard, defaultBehavior::run);
            builder.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
            builder.build(getActivity())
                   .show();
        } else {
            defaultBehavior.run();
        }
        return true;
    }

    //endregion

    //region methods

    /**
     * Used to update the fragment when the user has authenticated or choose a new value for the expansion.
     *
     * @param newExpansionAlarm state of the new expansion alarm.
     */
    @NotTested
    public void updateExpansion(@Nullable final ExpansionAlarm newExpansionAlarm) {
        if (newExpansionAlarm == null) {
            this.expansionsInteractor.update();
            return;
        }
        final ExpansionAlarm savedExpansionAlarm = this.alarm.getExpansionAlarm(newExpansionAlarm.getCategory());
        if (savedExpansionAlarm != null) {
            savedExpansionAlarm.setExpansionRange(newExpansionAlarm.getExpansionRange().max);
            savedExpansionAlarm.setEnabled(newExpansionAlarm.isEnabled());
            savedExpansionAlarm.setDisplayValue(this.expansionCategoryFormatter
                                                        .getFormattedAttributionValueRange(newExpansionAlarm.getCategory(),
                                                                                           newExpansionAlarm.getExpansionRange(),
                                                                                           getActivity()));
            final Expansion expansion = expansionsInteractor.getExpansion(savedExpansionAlarm.getCategory());
            if (expansion != null) {
                updateExpansion(expansion);
            }
        } else {
            this.alarm.getExpansions().add(newExpansionAlarm);
            final Expansion expansion = expansionsInteractor.getExpansion(newExpansionAlarm.getCategory());
            if (expansion != null) {
                updateExpansion(expansion);
            }

        }
        markDirty();
        this.expansionsInteractor.update();
    }

    /**
     * Call to update all UI views with the current state of {@link #alarm}.
     */
    @NotTested
    private void updateUIForAlarm() {
        this.presenterView.setTime(this.dateFormatter.formatAsAlarmTime(this.alarm.getTime(),
                                                                        this.use24Time));
        this.presenterView.setSmartAlarm(this.alarm.isSmart(),
                                         this::onSmartAlarmToggled);
        this.presenterView.setRepeatDaysTextView(this.alarm.getRepeatSummary(getActivity(),
                                                                             false));
        if (this.alarm.getSound() != null && !TextUtils.isEmpty(this.alarm.getSound().name)) {
            this.presenterView.setTone(this.alarm.getSound().name);
        } else {
            this.presenterView.setTone(null);
        }
        if (isNewAlarm()) {
            this.presenterView.showDeleteRow(null);
        } else {
            this.presenterView.showDeleteRow(this::onDeleteClicked);
        }
    }

    /**
     * Allow user to choose a new time for the alarm.
     *
     * @param ignored ignored
     */
    @NotTested
    private void onTimeClicked(final View ignored) {
        final TimePickerDialogFragment picker = TimePickerDialogFragment.newInstance(this.alarm.getTime(),
                                                                                     this.use24Time);
        picker.setTargetFragment(this, TIME_REQUEST_CODE);
        picker.showAllowingStateLoss(getFragmentManager(), TimePickerDialogFragment.TAG);
    }

    /**
     * Show a dialog explaining smart alarm.
     *
     * @param ignored ignored
     */
    @NotTested
    private void onHelpClicked(final View ignored) {
        WelcomeDialogFragment.show(getActivity(), R.xml.welcome_dialog_smart_alarm, false);
    }

    /**
     * Allow user to pick a tone (mp3 file) Sense will play when the alarm is started.
     *
     * @param ignored ignored
     */
    @NotTested
    private void onToneClicked(final View ignored) {
        if (this.alarm.getAlarmTones() == null) {
            this.smartAlarmInteractor.update();
            this.wantsTone = true;
            return;
        }
        ListActivity.startActivityForResult(
                this,
                SOUND_REQUEST_CODE,
                R.string.title_alarm_tone,
                this.alarm.getSound().getId(),
                this.alarm.getAlarmTones(),
                true);
        this.wantsTone = false;

    }

    /**
     * Allow user to change what days the alarm repeats on.
     *
     * @param ignored ignored
     */
    @NotTested
    private void onRepeatClicked(final View ignored) {
        final int firstCalendarDayOfWeek = Calendar.getInstance().getFirstDayOfWeek();
        final int firstJodaTimeDayOfWeek = DateFormatter.calendarDayToJodaTimeDay(firstCalendarDayOfWeek);
        final List<Integer> daysOfWeek = DateFormatter.getDaysOfWeek(firstJodaTimeDayOfWeek);
        final GenericListObject.GenericItemConverter converter = value -> new DateTime().withDayOfWeek(value).toString("EEEE");
        final ArrayList<Integer> list = new ArrayList<>(this.alarm.getDaysOfWeek());
        ListActivity.startActivityForResult(
                this,
                REPEAT_REQUEST_CODE,
                R.string.title_alarm_repeat,
                list,
                new GenericListObject(converter, daysOfWeek));
    }

    /**
     * Show prompt that will delete the alarm.
     *
     * @param ignored ignored
     */
    @NotTested
    private void onDeleteClicked(final View ignored) {
        final SenseAlertDialog.Builder builder = new SenseAlertDialog.Builder();
        builder.setMessage(R.string.dialog_message_confirm_delete_alarm);
        builder.setPositiveButton(R.string.action_delete, () -> {
            LoadingDialogFragment.show(getFragmentManager(),
                                       null, LoadingDialogFragment.DEFAULTS);
            bindAndSubscribe(this.smartAlarmInteractor.deleteSmartAlarm(this.index),
                             ignored2 ->
                             {
                                 sendAnalytics();
                                 cancelFlow();
                             },
                             this::presentError);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        builder.build(getActivity()).show();

    }

    /**
     * Callback when the user toggles the smart alarm. Will update the state of {@link #alarm}.
     *
     * @param ignored   not used
     * @param isChecked true turns the alarm into a smart alarm
     */
    @NotTested
    private void onSmartAlarmToggled(final CompoundButton ignored,
                                     final boolean isChecked) {
        this.alarm.setSmart(isChecked);
        markDirty();
    }

    /**
     * Save the current state of {@link #alarm}.
     * Will perform validation to make sure the alarm is in an acceptable state.
     */
    @NotTested
    private void saveAlarm() {
        if (!this.dirty && this.alarm.isEnabled()) {
            sendAnalytics();
            finishFlow();
            return;
        }

        this.alarm.setEnabled(true);

        // This is the only callback that does not have an outside state guard.
        this.stateSafeExecutor.execute(() -> {
            if (this.alarm.getSound() == null) {
                LoadingDialogFragment.close(getFragmentManager());
                new ErrorDialogFragment.PresenterBuilder(null)
                        .withMessage(StringRef.from(R.string.error_no_smart_alarm_tone))
                        .build()
                        .showAllowingStateLoss(getFragmentManager(),
                                               ErrorDialogFragment.TAG);
            } else if (this.smartAlarmInteractor.isAlarmTooSoon(alarm)) {
                LoadingDialogFragment.close(getFragmentManager());

                new ErrorDialogFragment.PresenterBuilder(null)
                        .withMessage(StringRef.from(R.string.error_alarm_too_soon))
                        .build()
                        .showAllowingStateLoss(getFragmentManager(),
                                               ErrorDialogFragment.TAG);
            } else {
                if (this.alarm.getDaysOfWeek().isEmpty()) {
                    this.alarm.setRingOnce();
                }
                final Observable<VoidResponse> saveOperation;
                if (isNewAlarm()) {
                    saveOperation = this.smartAlarmInteractor.addSmartAlarm(this.alarm);
                } else {
                    saveOperation = this.smartAlarmInteractor.saveSmartAlarm(this.index,
                                                                             this.alarm);
                }

                LoadingDialogFragment.show(getFragmentManager(),
                                           null, LoadingDialogFragment.DEFAULTS);
                bindAndSubscribe(saveOperation, ignored -> {
                    sendAnalytics();
                    LoadingDialogFragment.close(getFragmentManager());
                    finishFlow();
                }, this::presentError);
            }
        });

    }

    /**
     * Show the error dialog
     *
     * @param e checked for 412 status or instance of {@link is.hello.sense.interactors.SmartAlarmInteractor.DayOverlapError}
     */
    @NotTested
    public void presentError(@NonNull final Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        final ErrorDialogFragment.PresenterBuilder errorDialogBuilder = new ErrorDialogFragment.PresenterBuilder(e);
        if (e instanceof SmartAlarmInteractor.DayOverlapError) {
            errorDialogBuilder.withMessage(StringRef.from(R.string.error_smart_alarm_day_overlap));
        } else if (ApiException.statusEquals(e, 412)) {
            errorDialogBuilder.withMessage(StringRef.from(getString(R.string.error_smart_alarm_requires_device)));
        }
        errorDialogBuilder.build()
                          .showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }


    /**
     * Use when the state of the alarm has changed.
     */
    @NotTested
    private void markDirty() {
        this.dirty = true;
    }

    /**
     * Bind function for {@link #smartAlarmInteractor#availableAlarmSounds()}.
     *
     * @param sounds different sounds (mp3 files) Sense can play.
     */
    @NotTested
    private void bindSmartAlarmSounds(@Nullable final ArrayList<Alarm.Sound> sounds) {
        if (sounds != null && !sounds.isEmpty()) {
            this.alarm.setAlarmTones(sounds);
            if (this.alarm.getSound() == null) {
                final Alarm.Sound sound = sounds.get(0);
                this.alarm.setSound(sound);
                if (this.skipUI) {
                    saveAlarm();
                    return;
                }
                this.presenterView.setTone(sound.name);
            }
        }
        if (this.wantsTone) {
            onToneClicked(null);
        }
    }

    /**
     * Bind function for {@link #expansionsInteractor#expansions}.
     *
     * @param expansions list of expansions that can be modified for this alarm. Only supports
     *                   {@link Category#LIGHT} and {@link Category#TEMPERATURE}
     */
    @NotTested
    private void bindExpansions(@NonNull final ArrayList<Expansion> expansions) {
        for (final Expansion expansion : expansions) {
            updateExpansion(expansion);
        }
    }

    /**
     * Update the current state of an expansion in the view.
     *
     * @param expansion expansions new state.
     */
    @NotTested
    private void updateExpansion(@NonNull final Expansion expansion) {
        final boolean enabled = expansion.getState() != State.NOT_AVAILABLE;
        final int defaultValue = this.expansionCategoryFormatter.getDisplayValueResFromState(expansion.getState());
        final String value;
        final View.OnClickListener clickListener;
        if (expansion.getState() == State.CONNECTED_ON) {
            final ExpansionAlarm expansionAlarm = this.alarm.getExpansionAlarm(expansion.getCategory());
            if (expansionAlarm == null || !expansionAlarm.isEnabled()) {
                value = getString(defaultValue);
                clickListener = (ignored) -> redirectToExpansionPicker(expansion);
            } else {
                value = this.expansionCategoryFormatter.getFormattedValueRange(expansion.getCategory(),
                                                                               expansionAlarm.getExpansionRange(),
                                                                               getActivity());
                clickListener = (ignored) -> redirectToExpansionPicker(expansionAlarm);
            }
        } else {
            // Not connected. Need to authorize.
            value = getString(defaultValue);
            clickListener = (ignored) -> redirectToExpansionDetail(expansion.getId());
        }
        switch (expansion.getCategory()) {
            case TEMPERATURE:
                this.presenterView.setThermoExpansion(enabled,
                                                      value,
                                                      clickListener);
                break;
            case LIGHT:
                this.presenterView.setLightExpansion(enabled,
                                                     value,
                                                     clickListener);
                break;
        }
    }

    /**
     * Show a picker for the given expansion alarm.
     *
     * @param expansionAlarm alarm to be changed
     */
    @NotTested
    private void redirectToExpansionPicker(@NonNull final ExpansionAlarm expansionAlarm) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_EXPANSION_ALARM, expansionAlarm);
        finishFlowWithResult(RESULT_PICKER_EXPANSION_ALARM, intent);
    }

    /**
     * Show a picker for the given expansion.
     *
     * @param expansion expansion to be modified
     */
    @NotTested
    private void redirectToExpansionPicker(@NonNull final Expansion expansion) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_EXPANSION, expansion);
        finishFlowWithResult(RESULT_PICKER_EXPANSION, intent);
    }

    /**
     * Show authorization for the given expansion ID.
     *
     * @param expansionId expansion id to authenticate
     */
    @NotTested
    private void redirectToExpansionDetail(final long expansionId) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_EXPANSION_ID, expansionId);
        finishFlowWithResult(RESULT_AUTHENTICATE_EXPANSION, intent);
    }

    /**
     * Show an error when we fail to fetch expansions from {@link #expansionsInteractor#expansions}.
     *
     * @param ignored ignored
     */
    @NotTested
    private void bindExpansionError(final Throwable ignored) {
        new SenseAlertDialog.Builder()
                .setTitle(R.string.expansion_not_loaded_title)
                .setMessage(R.string.expansion_not_loaded_message)
                .setPositiveButton(R.string.action_ok, () -> {

                })
                .setNegativeButton(R.string.label_having_trouble, () -> {
                    //todo should alert dialog pop up here?
                })
                .build(getActivity())
                .show();
    }

    /**
     * @return true if the user is adding a new alarm.
     * False if editing an existing alarm.
     */
    @NotTested
    private boolean isNewAlarm() {
        return this.index == Constants.NONE;
    }


    /**
     * Send analytics for the current state of the alarm. Should be done when finished modifying.
     */
    public void sendAnalytics() {
        final String daysRepeated = TextUtils.join(", ", alarm.getSortedDaysOfWeek());
        final Properties properties =
                Analytics.createProperties(Analytics.Backside.PROP_ALARM_ENABLED, alarm.isEnabled(),
                                           Analytics.Backside.PROP_ALARM_IS_SMART, alarm.isSmart(),
                                           Analytics.Backside.PROP_ALARM_DAYS_REPEATED, daysRepeated,
                                           Analytics.Backside.PROP_ALARM_HOUR, alarm.getHourOfDay(),
                                           Analytics.Backside.PROP_ALARM_MINUTE, alarm.getMinuteOfHour());
        Analytics.trackEvent(Analytics.Backside.EVENT_ALARM_SAVED, properties);
    }
    //endregion
}
