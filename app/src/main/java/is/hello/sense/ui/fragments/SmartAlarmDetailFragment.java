package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import com.segment.analytics.Properties;

import org.joda.time.LocalTime;

import java.util.List;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.AlarmRepeatDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.SmartAlarmSoundDialogFragment;
import is.hello.sense.ui.dialogs.TimePickerDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import rx.Observable;


public class SmartAlarmDetailFragment extends InjectionFragment {
    private static final int TIME_REQUEST_CODE = 0x747;
    private static final int SOUND_REQUEST_CODE = 0x50;
    private static final int REPEAT_REQUEST_CODE = 0x59;

    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;
    @Inject SmartAlarmPresenter smartAlarmPresenter;

    private boolean dirty = false;

    private Alarm alarm;
    private int index = SmartAlarmDetailActivity.INDEX_NEW;
    private boolean use24Time = false;

    private TextView time;
    private TextView enabled;
    private TextView toneName;
    private TextView repeatDays;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SmartAlarmDetailActivity activity = getDetailActivity();
        this.alarm = activity.getAlarm();
        this.index = activity.getIndex();

        if (savedInstanceState != null) {
            this.dirty = savedInstanceState.getBoolean("dirty", false);
        } else {
            this.dirty = (index == SmartAlarmDetailActivity.INDEX_NEW);
        }

        smartAlarmPresenter.update();
        addPresenter(smartAlarmPresenter);
        addPresenter(preferences);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_smart_alarm_detail, container, false);

        this.time = (TextView) view.findViewById(R.id.fragment_smart_alarm_detail_time);
        updateTime();

        final View timeContainer = view.findViewById(R.id.fragment_smart_alarm_detail_time_container);
        Views.setSafeOnClickListener(timeContainer, stateSafeExecutor, this::selectNewTime);


        this.enabled = (TextView) view.findViewById(R.id.fragment_smart_alarm_detail_enabled_text);
        if (alarm.isEnabled()) {
            enabled.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_alarm_enabled, 0, 0, 0);
        } else {
            enabled.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_alarm_disabled, 0, 0, 0);
        }

        final CompoundButton enabledToggle = (CompoundButton) view.findViewById(R.id.fragment_smart_alarm_detail_enabled_toggle);
        enabledToggle.setChecked(alarm.isEnabled());
        enabledToggle.setOnCheckedChangeListener((button, isEnabled) -> {
            Analytics.trackEvent(Analytics.Backside.EVENT_ALARM_ON_OFF, null);
            alarm.setEnabled(isEnabled);
            if (isEnabled) {
                enabled.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_alarm_enabled, 0, 0, 0);
            } else {
                enabled.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_alarm_disabled, 0, 0, 0);
            }
            markDirty();
        });

        final View enabledRow = view.findViewById(R.id.fragment_smart_alarm_detail_enabled);
        enabledRow.setOnClickListener(ignored -> enabledToggle.toggle());


        final CompoundButton smartToggle = (CompoundButton) view.findViewById(R.id.fragment_smart_alarm_detail_smart_switch);
        smartToggle.setChecked(alarm.isSmart());
        smartToggle.setOnCheckedChangeListener((button, checked) -> {
            alarm.setSmart(checked);
            markDirty();
        });

        final View smartRow = view.findViewById(R.id.fragment_smart_alarm_detail_smart);
        smartRow.setOnClickListener(ignored -> smartToggle.toggle());

        final ImageButton smartHelp = (ImageButton) view.findViewById(R.id.fragment_smart_alarm_detail_smart_help);
        final Drawable smartHelpDrawable = smartHelp.getDrawable().mutate();
        final int accent = getResources().getColor(R.color.light_accent);
        final int dimmedAccent = Drawing.colorWithAlpha(accent, 178);
        Drawables.setTintColor(smartHelpDrawable, dimmedAccent);
        smartHelp.setImageDrawable(smartHelpDrawable);
        Views.setSafeOnClickListener(smartHelp, stateSafeExecutor, this::showSmartAlarmIntro);


        final View soundRow = view.findViewById(R.id.fragment_smart_alarm_detail_tone);
        Views.setSafeOnClickListener(soundRow, stateSafeExecutor, this::selectSound);

        this.toneName = (TextView) soundRow.findViewById(R.id.fragment_smart_alarm_detail_tone_name);
        if (alarm.getSound() != null && !TextUtils.isEmpty(alarm.getSound().name)) {
            toneName.setText(alarm.getSound().name);
        } else {
            toneName.setText(R.string.no_sound_placeholder);
        }


        final View repeatRow = view.findViewById(R.id.fragment_smart_alarm_detail_repeat);
        Views.setSafeOnClickListener(repeatRow, stateSafeExecutor, this::selectRepeatDays);

        this.repeatDays = (TextView) repeatRow.findViewById(R.id.fragment_smart_alarm_detail_repeat_days);
        repeatDays.setText(alarm.getRepeatSummary(getActivity()));

        final View deleteRow = view.findViewById(R.id.fragment_smart_alarm_detail_delete);
        Views.setSafeOnClickListener(deleteRow, stateSafeExecutor, this::deleteAlarm);

        if (this.index == SmartAlarmDetailActivity.INDEX_NEW) {
            final View deleteRowDivider = view.findViewById(R.id.fragment_smart_alarm_detail_delete_divider);
            deleteRowDivider.setVisibility(View.GONE);
            deleteRow.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(preferences.observableUse24Time(),
                         newValue -> {
                             this.use24Time = newValue;
                             updateTime();
                         },
                         Functions.LOG_ERROR);

        bindAndSubscribe(smartAlarmPresenter.availableAlarmSounds(),
                         sounds -> {
                             if (alarm.getSound() == null && !sounds.isEmpty()) {
                                 final Alarm.Sound sound = sounds.get(0);
                                 alarm.setSound(sound);
                                 toneName.setText(sound.name);

                                 if (getDetailActivity().skipUI()) {
                                     saveAlarm();
                                 }
                             }
                         },
                         Functions.LOG_ERROR);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getDetailActivity().skipUI()) {
            LoadingDialogFragment.show(getFragmentManager(),
                                       null, LoadingDialogFragment.DEFAULTS);
        } else {
            WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_alarm, false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TIME_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            final int hour = data.getIntExtra(TimePickerDialogFragment.RESULT_HOUR, 7);
            final int minute = data.getIntExtra(TimePickerDialogFragment.RESULT_MINUTE, 30);
            alarm.setTime(new LocalTime(hour, minute));
            updateTime();

            markDirty();
        } else if (requestCode == SOUND_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            final Alarm.Sound selectedSound = (Alarm.Sound) data.getSerializableExtra(SmartAlarmSoundDialogFragment.ARG_SELECTED_SOUND);
            alarm.setSound(selectedSound);
            toneName.setText(selectedSound.name);

            markDirty();
        } else if (requestCode == REPEAT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            final List<Integer> selectedDays = data.getIntegerArrayListExtra(AlarmRepeatDialogFragment.RESULT_DAYS);
            alarm.setDaysOfWeek(selectedDays);
            repeatDays.setText(alarm.getRepeatSummary(getActivity()));

            markDirty();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("dirty", dirty);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        smartAlarmPresenter.forgetAvailableAlarmSoundsCache();
    }


    private SmartAlarmDetailActivity getDetailActivity() {
        return (SmartAlarmDetailActivity) getActivity();
    }

    public void updateTime() {
        final CharSequence formattedTime = dateFormatter.formatAsAlarmTime(alarm.getTime(), use24Time);
        time.setText(formattedTime);
    }

    public void selectNewTime(@NonNull View sender) {
        final TimePickerDialogFragment picker = TimePickerDialogFragment.newInstance(alarm.getTime(),
                                                                                     use24Time);
        picker.setTargetFragment(this, TIME_REQUEST_CODE);
        picker.showAllowingStateLoss(getFragmentManager(), TimePickerDialogFragment.TAG);
    }

    public void selectSound(@NonNull View sender) {
        final SmartAlarmSoundDialogFragment dialogFragment =
                SmartAlarmSoundDialogFragment.newInstance(alarm.getSound());
        dialogFragment.setTargetFragment(this, SOUND_REQUEST_CODE);
        dialogFragment.showAllowingStateLoss(getFragmentManager(),
                                             SmartAlarmSoundDialogFragment.TAG);
    }

    public void selectRepeatDays(@NonNull View sender) {
        final AlarmRepeatDialogFragment dialogFragment =
                AlarmRepeatDialogFragment.newInstance(alarm.getDaysOfWeek());
        dialogFragment.setTargetFragment(this, REPEAT_REQUEST_CODE);
        dialogFragment.showAllowingStateLoss(getFragmentManager(),
                                             AlarmRepeatDialogFragment.TAG);
    }

    public void showSmartAlarmIntro(@NonNull View sender) {
        WelcomeDialogFragment.show(getActivity(), R.xml.welcome_dialog_smart_alarm, false);
    }


    public void deleteAlarm(@NonNull View sender) {
        final SenseAlertDialog confirmDelete = new SenseAlertDialog(getActivity());
        confirmDelete.setMessage(R.string.dialog_message_confirm_delete_alarm);
        confirmDelete.setPositiveButton(R.string.action_delete, (dialog, which) -> {
            LoadingDialogFragment.show(getFragmentManager(),
                    null, LoadingDialogFragment.DEFAULTS);
            bindAndSubscribe(smartAlarmPresenter.deleteSmartAlarm(index),
                             ignored -> finish(),
                             this::presentError);
        });
        confirmDelete.setNegativeButton(android.R.string.cancel, null);
        confirmDelete.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        confirmDelete.show();
    }

    public void saveAlarm() {
        if (!dirty) {
            finish();
            return;
        }

        // This is the only callback that does not have an outside state guard.
        stateSafeExecutor.execute(() -> {
            if (alarm.getSound() == null) {
                LoadingDialogFragment.close(getFragmentManager());

                final ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder()
                        .withMessage(StringRef.from(R.string.error_no_smart_alarm_tone))
                        .build();
                dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            } else if (smartAlarmPresenter.isAlarmTooSoon(alarm)) {
                LoadingDialogFragment.close(getFragmentManager());

                final ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder()
                        .withMessage(StringRef.from(R.string.error_alarm_too_soon))
                        .build();
                dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            } else {
                if (alarm.getDaysOfWeek().isEmpty()) {
                    alarm.setRingOnce();
                }

                final Observable<VoidResponse> saveOperation;
                if (index == SmartAlarmDetailActivity.INDEX_NEW) {
                    saveOperation = smartAlarmPresenter.addSmartAlarm(alarm);
                } else {
                    saveOperation = smartAlarmPresenter.saveSmartAlarm(index, alarm);
                }

                LoadingDialogFragment.show(getFragmentManager(),
                                           null, LoadingDialogFragment.DEFAULTS);
                bindAndSubscribe(saveOperation, ignored -> finish(), this::presentError);
            }
        });
    }

    private void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void finish() {
        final String daysRepeated = TextUtils.join(", ", alarm.getSortedDaysOfWeek());
        final Properties properties =
                Analytics.createProperties(Analytics.Backside.PROP_ALARM_ENABLED, alarm.isEnabled(),
                                           Analytics.Backside.PROP_ALARM_IS_SMART, alarm.isSmart(),
                                           Analytics.Backside.PROP_ALARM_DAYS_REPEATED, daysRepeated);
        Analytics.trackEvent(Analytics.Backside.EVENT_ALARM_SAVED, properties);

        LoadingDialogFragment.close(getFragmentManager());
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getResources());
        if (e instanceof SmartAlarmPresenter.DayOverlapError) {
            errorDialogBuilder.withMessage(StringRef.from(R.string.error_smart_alarm_day_overlap));
        } else if (ApiException.statusEquals(e, 400)) {
            errorDialogBuilder.withMessage(StringRef.from(getString(R.string.error_smart_alarm_clock_drift)));
            errorDialogBuilder.withAction(new Intent(Settings.ACTION_DATE_SETTINGS), R.string.action_settings);
        } else if (ApiException.statusEquals(e, 412)) {
            errorDialogBuilder.withMessage(StringRef.from(getString(R.string.error_smart_alarm_requires_device)));
        }
        final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }
}
