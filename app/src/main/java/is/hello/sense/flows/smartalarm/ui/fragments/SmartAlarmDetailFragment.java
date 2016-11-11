package is.hello.sense.flows.smartalarm.ui.fragments;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.ExpansionAlarm;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.flows.expansions.interactors.ExpansionsInteractor;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.expansions.ui.activities.ExpansionValuePickerActivity;
import is.hello.sense.flows.expansions.utils.ExpansionCategoryFormatter;
import is.hello.sense.flows.smartalarm.ui.views.SmartAlarmDetailView;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.HasVoiceInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SmartAlarmInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.dialogs.TimePickerDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.GenericListObject;

public class SmartAlarmDetailFragment extends PresenterFragment<SmartAlarmDetailView> {
    //region static functions and fields
    public static final String ARG_ALARM = SmartAlarmDetailFragment.class.getName() + ".ARG_ALARM";
    public static final String ARG_INDEX = SmartAlarmDetailFragment.class.getName() + ".ARG_INDEX";
    public static final String ARG_SKIP = SmartAlarmDetailFragment.class.getName() + ".ARG_SKIP";
    public static final String KEY_ALARM = SmartAlarmDetailFragment.class.getName() + ".KEY_ALARM";
    public static final String KEY_INDEX = SmartAlarmDetailFragment.class.getName() + ".KEY_INDEX";
    public static final String KEY_SKIP = SmartAlarmDetailFragment.class.getName() + ".KEY_SKIP";
    public static final String KEY_DIRTY = SmartAlarmDetailFragment.class.getName() + ".KEY_DIRTY";
    private static final int TIME_REQUEST_CODE = 0x747;
    private static final int SOUND_REQUEST_CODE = 0x50;
    private static final int REPEAT_REQUEST_CODE = 0x59;
    private static final int EXPANSION_VALUE_REQUEST_CODE = 0x69;

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

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SKIP, this.skipUI); // probably won't ever be needed.
        outState.putSerializable(KEY_ALARM, this.alarm);
        outState.putInt(KEY_INDEX, this.index);
        outState.putBoolean(KEY_DIRTY, this.dirty);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.dirty = savedInstanceState.getBoolean(KEY_DIRTY, false);
            this.alarm = (Alarm) savedInstanceState.getSerializable(KEY_ALARM);
            this.skipUI = savedInstanceState.getBoolean(KEY_SKIP);
            this.index = savedInstanceState.getInt(KEY_INDEX);
        } else {
            final Bundle args = getArguments();
            if (args == null) { // should never be allowed to happen
                cancelFlow();
                return;
            }
            this.skipUI = args.getBoolean(ARG_SKIP, false);
            this.alarm = (Alarm) args.getSerializable(ARG_ALARM);
            this.index = args.getInt(ARG_INDEX);
            this.dirty = (index == Constants.NONE);
        }
        addInteractor(this.smartAlarmInteractor);
        addInteractor(this.hasVoiceInteractor);
        addInteractor(this.preferences);
        addInteractor(this.expansionsInteractor);
        setRetainInstance(true);
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateUIForAlarm();
        bindAndSubscribe(this.expansionsInteractor.expansions,
                         this::bindExpansions,
                         this::bindExpansionError);
        bindAndSubscribe(this.hasVoiceInteractor.hasVoice,
                         hasVoice -> {
                             if (hasVoice) {
                                 this.presenterView.showExpansionsContainer();
                                 this.expansionsInteractor.update();
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

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == EXPANSION_VALUE_REQUEST_CODE) {
                this.expansionsInteractor.update();
            }
            return;
        }

        if (data == null) {
            return;
        }

        if (requestCode == TIME_REQUEST_CODE) {
            final int hour = data.getIntExtra(TimePickerDialogFragment.RESULT_HOUR, 7);
            final int minute = data.getIntExtra(TimePickerDialogFragment.RESULT_MINUTE, 30);
            this.alarm.setTime(new LocalTime(hour, minute));
            updateUIForAlarm();

            markDirty();
        } else if (requestCode == SOUND_REQUEST_CODE) {
            final int soundId = data.getIntExtra(ListActivity.VALUE_ID, -1);
            if (soundId != -1) {
                final Alarm.Sound selectedSound = alarm.getAlarmSoundWithId(soundId);
                if (selectedSound != null) {
                    this.alarm.setSound(selectedSound);
                    this.presenterView.setTone(selectedSound.name);
                }
            }
            markDirty();
        } else if (requestCode == REPEAT_REQUEST_CODE) {
            final List<Integer> selectedDays = data.getIntegerArrayListExtra(ListActivity.VALUE_ID);
            this.alarm.setDaysOfWeek(selectedDays);
            this.presenterView.setRepeatDaysTextView(alarm.getRepeatSummary(getActivity(), false));
            markDirty();
        } else if (requestCode == EXPANSION_VALUE_REQUEST_CODE) {
            final ExpansionAlarm expansionAlarm = (ExpansionAlarm) data.getSerializableExtra(ExpansionValuePickerActivity.EXTRA_EXPANSION_ALARM);
            final ExpansionAlarm savedExpansionAlarm = alarm.getExpansionAlarm(expansionAlarm.getCategory());
            if (savedExpansionAlarm != null) {
                savedExpansionAlarm.setExpansionRange(expansionAlarm.getExpansionRange().max);
                savedExpansionAlarm.setEnabled(expansionAlarm.isEnabled());
                final Expansion expansion = getExpansion(savedExpansionAlarm.getCategory());
                if (expansion != null) {
                    updateExpansion(expansion);
                }
            } else {
                this.alarm.getExpansions().add(expansionAlarm);
                final Expansion expansion = getExpansion(expansionAlarm.getCategory());
                if (expansion != null) {
                    updateExpansion(expansion);
                }

            }
            this.expansionsInteractor.update();
            markDirty();
        }
    }
    //endregion

    //region methods
    private Expansion getExpansion(final Category category) {
        if (!expansionsInteractor.expansions.hasValue()) {
            return null;
        }
        final List<Expansion> expansions = expansionsInteractor.expansions.getValue();
        for (final Expansion expansion : expansions) {
            if (expansion.getCategory() == category) {
                return expansion;
            }
        }
        return null;
    }

    private void updateUIForAlarm() {
        final CharSequence formattedTime = this.dateFormatter.formatAsAlarmTime(this.alarm.getTime(),
                                                                                this.use24Time);
        this.presenterView.setTime(formattedTime);
        this.presenterView.setSmartAlarm(this.alarm.isSmart(), this::onSmartAlarmToggled);
        this.presenterView.setRepeatDaysTextView(this.alarm.getRepeatSummary(getActivity(), false));
        if (alarm.getSound() != null && !TextUtils.isEmpty(alarm.getSound().name)) {
            this.presenterView.setTone(alarm.getSound().name);
        } else {
            this.presenterView.setTone(null);
        }
        if (this.index == Constants.NONE) {
            this.presenterView.showDeleteRow(null);
        } else {
            this.presenterView.showDeleteRow(this::onDeleteClicked);
        }
    }

    private void onTimeClicked(final View ignored) {
        final TimePickerDialogFragment picker = TimePickerDialogFragment.newInstance(alarm.getTime(),
                                                                                     use24Time);
        picker.setTargetFragment(this, TIME_REQUEST_CODE);
        picker.showAllowingStateLoss(getFragmentManager(), TimePickerDialogFragment.TAG);

    }

    private void onHelpClicked(final View ignored) {
        WelcomeDialogFragment.show(getActivity(), R.xml.welcome_dialog_smart_alarm, false);
    }

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

    private void onDeleteClicked(final View ignored) {

    }

    private void onSmartAlarmToggled(final CompoundButton ignored,
                                     final boolean isChecked) {
        this.alarm.setSmart(isChecked);
        markDirty();
    }

    private void saveAlarm() {

    }

    private void markDirty() {
        this.dirty = true;
    }

    void bindSmartAlarmSounds(@Nullable final ArrayList<Alarm.Sound> sounds) {
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

    private void bindExpansions(@NonNull final ArrayList<Expansion> expansions) {
        // Set initial click listener
        for (final Expansion expansion : expansions) {
            updateExpansion(expansion);
        }
    }

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

    private void redirectToExpansionPicker(@NonNull final ExpansionAlarm expansionAlarm) {

        startActivityForResult(ExpansionValuePickerActivity.getIntent(getActivity(),
                                                                      expansionAlarm),
                               EXPANSION_VALUE_REQUEST_CODE);
    }

    public void redirectToExpansionPicker(@NonNull final Expansion expansion) {

        startActivityForResult(ExpansionValuePickerActivity.getIntent(getActivity(),
                                                                      expansion,
                                                                      false),
                               EXPANSION_VALUE_REQUEST_CODE);
    }

    private void redirectToExpansionDetail(final long expansionId) {
        startActivityForResult(ExpansionSettingsActivity.getExpansionDetailIntent(getActivity(),
                                                                                  expansionId),
                               EXPANSION_VALUE_REQUEST_CODE);
    }

    private void bindExpansionError(final Throwable throwable) {
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
    //endregion
}
