package is.hello.sense.ui.fragments.sounds;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

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
import is.hello.sense.flows.expansions.interactors.ExpansionsInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SmartAlarmInteractor;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.TimePickerDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.GenericListObject;
import rx.Observable;


public class SmartAlarmDetailFragment extends InjectionFragment {
    private static final int TIME_REQUEST_CODE = 0x747;
    private static final int SOUND_REQUEST_CODE = 0x50;
    private static final int REPEAT_REQUEST_CODE = 0x59;

    @Inject
    DateFormatter dateFormatter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    SmartAlarmInteractor smartAlarmPresenter;
    @Inject
    ExpansionsInteractor expansionsInteractor;


    private boolean dirty = false;
    private boolean wantsTone = false;

    private Alarm alarm;
    private int index = SmartAlarmDetailActivity.INDEX_NEW;
    private boolean use24Time = false;

    private TextView time;
    private TextView toneName;
    private TextView repeatDays;
    private View expansionLightsRow;
    private TextView expansionLightsValue;
    private ImageView expansionLightsErrorView;
    private CompoundButton expansionLightsToggle;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
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
        addPresenter(expansionsInteractor);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_smart_alarm_detail, container, false);

        this.time = (TextView) view.findViewById(R.id.fragment_smart_alarm_detail_time);
        updateTime();

        final View timeContainer = view.findViewById(R.id.fragment_smart_alarm_detail_time_container);
        Views.setSafeOnClickListener(timeContainer, stateSafeExecutor, this::selectNewTime);


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
        final int accent = ContextCompat.getColor(getActivity(), R.color.light_accent);
        final int dimmedAccent = Drawing.colorWithAlpha(accent, 178);
        Drawables.setTintColor(smartHelpDrawable, dimmedAccent);
        smartHelp.setImageDrawable(smartHelpDrawable);
        Views.setSafeOnClickListener(smartHelp, stateSafeExecutor, this::showSmartAlarmIntro);


        final View soundRow = view.findViewById(R.id.fragment_smart_alarm_detail_tone);
        Views.setSafeOnClickListener(soundRow, stateSafeExecutor, v -> selectTone());

        this.toneName = (TextView) soundRow.findViewById(R.id.fragment_smart_alarm_detail_tone_name);
        if (alarm.getSound() != null && !TextUtils.isEmpty(alarm.getSound().name)) {
            toneName.setText(alarm.getSound().name);
        } else {
            toneName.setText(R.string.no_sound_placeholder);
        }


        final View repeatRow = view.findViewById(R.id.fragment_smart_alarm_detail_repeat);
        Views.setSafeOnClickListener(repeatRow, stateSafeExecutor, this::selectRepeatDays);

        this.repeatDays = (TextView) repeatRow.findViewById(R.id.fragment_smart_alarm_detail_repeat_days);
        repeatDays.setText(alarm.getRepeatSummary(getActivity(), false));

        expansionLightsRow = view.findViewById(R.id.fragment_smart_alarm_detail_lights);
        expansionLightsValue = (TextView) view.findViewById(R.id.fragment_smart_alarm_detail_lights_value);
        expansionLightsToggle = (CompoundButton) view.findViewById(R.id.fragment_smart_alarm_detail_lights_switch);
        expansionLightsErrorView = (ImageView) view.findViewById(R.id.fragment_smart_alarm_detail_lights_error);
        expansionLightsErrorView.setVisibility(View.GONE);

        expansionLightsErrorView.setOnClickListener(this::onLightErrorIconPress);

        final ImageView lightHelpIcon = (ImageView) view.findViewById(R.id.fragment_smart_alarm_detail_lights_help);
        Views.setSafeOnClickListener(lightHelpIcon, stateSafeExecutor, this::onLightHelpIconPress);

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
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(expansionsInteractor.expansions,
                         this::bindExpansions,
                         this::bindExpansionsThrowable);

        bindAndSubscribe(preferences.observableBoolean(PreferencesInteractor.HAS_VOICE, false),
                         enabled -> {
                             if (enabled) {
                                 expansionLightsRow.setVisibility(View.VISIBLE);
                                 expansionsInteractor.update();
                             } else {
                                 expansionLightsRow.setVisibility(View.GONE);
                             }
                         },
                         Functions.LOG_ERROR);

        bindAndSubscribe(preferences.observableUse24Time(),
                         newValue -> {
                             this.use24Time = newValue;
                             updateTime();
                         },
                         Functions.LOG_ERROR);

        bindAndSubscribe(smartAlarmPresenter.availableAlarmSounds(),
                         sounds -> {
                             if (!sounds.isEmpty()) {
                                 alarm.setAlarmTones(sounds);
                                 if (alarm.getSound() == null) {
                                     final Alarm.Sound sound = sounds.get(0);
                                     alarm.setSound(sound);
                                     toneName.setText(sound.name);
                                     if (getDetailActivity().skipUI()) {
                                         saveAlarm();
                                     }
                                 }
                             }
                             if (wantsTone) {
                                 selectTone();
                             }
                         },
                         Functions.LOG_ERROR);
    }

    @Override
    public void onResume() {
        super.onResume();
        expansionLightsToggle.setVisibility(View.GONE);
        expansionsInteractor.update();

        if (getDetailActivity().skipUI()) {
            LoadingDialogFragment.show(getFragmentManager(),
                                       null, LoadingDialogFragment.DEFAULTS);
        } else {
            WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_alarm, false);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == TIME_REQUEST_CODE) {
            final int hour = data.getIntExtra(TimePickerDialogFragment.RESULT_HOUR, 7);
            final int minute = data.getIntExtra(TimePickerDialogFragment.RESULT_MINUTE, 30);
            alarm.setTime(new LocalTime(hour, minute));
            updateTime();

            markDirty();
        } else if (requestCode == SOUND_REQUEST_CODE) {
            final int soundId = data.getIntExtra(ListActivity.VALUE_ID, -1);
            if (soundId != -1) {
                final Alarm.Sound selectedSound = alarm.getAlarmSoundWithId(soundId);
                if (selectedSound != null) {
                    alarm.setSound(selectedSound);
                    toneName.setText(selectedSound.name);
                }
            }
            markDirty();
        } else if (requestCode == REPEAT_REQUEST_CODE) {
            final List<Integer> selectedDays = data.getIntegerArrayListExtra(ListActivity.VALUE_ID);
            alarm.setDaysOfWeek(selectedDays);
            repeatDays.setText(alarm.getRepeatSummary(getActivity(), false));
            markDirty();
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
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

    private void bindExpansions(@NonNull final List<Expansion> expansions) {
        expansionLightsErrorView.setVisibility(View.GONE);
        for (final Expansion expansion : expansions) {
            if (Category.LIGHT == expansion.getCategory()) {
                switch (expansion.getState()) {
                    case CONNECTED_OFF:
                        showLightToggleButton(false);
                        break;
                    case CONNECTED_ON:
                        showLightToggleButton(true);
                        break;
                    default:
                        hideLightToggleButton(expansion.getState().displayValue);
                }
                return;
            }
        }
    }

    private void bindExpansionsThrowable(@NonNull final Throwable throwable) {
        expansionLightsToggle.setVisibility(View.GONE);
        expansionLightsValue.setVisibility(View.GONE);
        if (expansionLightsErrorView.getVisibility() == View.VISIBLE) {
            this.showExpansionError();
        }
        expansionLightsErrorView.setVisibility(View.VISIBLE);

    }

    public void updateTime() {
        final CharSequence formattedTime = dateFormatter.formatAsAlarmTime(alarm.getTime(), use24Time);
        time.setText(formattedTime);
    }

    public void selectNewTime(@NonNull final View sender) {
        final TimePickerDialogFragment picker = TimePickerDialogFragment.newInstance(alarm.getTime(),
                                                                                     use24Time);
        picker.setTargetFragment(this, TIME_REQUEST_CODE);
        picker.showAllowingStateLoss(getFragmentManager(), TimePickerDialogFragment.TAG);
    }

    public void selectTone() {
        if (alarm.getAlarmTones() == null) {
            smartAlarmPresenter.update();
            wantsTone = true;
            return;
        }
        ListActivity.startActivityForResult(
                this,
                SOUND_REQUEST_CODE,
                R.string.title_alarm_tone,
                alarm.getSound().getId(),
                alarm.getAlarmTones(),
                true);
        wantsTone = false;
    }

    public void selectRepeatDays(@NonNull final View sender) {

        final int firstCalendarDayOfWeek = Calendar.getInstance().getFirstDayOfWeek();
        final int firstJodaTimeDayOfWeek = DateFormatter.calendarDayToJodaTimeDay(firstCalendarDayOfWeek);
        final List<Integer> daysOfWeek = DateFormatter.getDaysOfWeek(firstJodaTimeDayOfWeek);
        final GenericListObject.GenericItemConverter converter = value -> new DateTime().withDayOfWeek(value).toString("EEEE");
        final ArrayList<Integer> list = new ArrayList<>(alarm.getDaysOfWeek());
        ListActivity.startActivityForResult(
                this,
                REPEAT_REQUEST_CODE,
                R.string.title_alarm_repeat,
                list,
                new GenericListObject(converter, daysOfWeek));
    }

    public void showSmartAlarmIntro(@NonNull final View sender) {
        WelcomeDialogFragment.show(getActivity(), R.xml.welcome_dialog_smart_alarm, false);
    }


    public void deleteAlarm(@NonNull final View sender) {
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

    //region Expansion Light Row

    public void onLightHelpIconPress(@NonNull final View view) {
        WelcomeDialogFragment.show(getActivity(),
                                   R.xml.welcome_dialog_expansions,
                                   true);

    }

    public void onLightErrorIconPress(@NonNull final View view) {
        expansionsInteractor.update();
    }

    private void showExpansionError() {
        new SenseAlertDialog.Builder()
                .setTitle(R.string.expansion_not_loaded_title)
                .setMessage(R.string.expansion_not_loaded_message)
                .setPositiveButton(R.string.action_ok, () -> {

                })
                .setNegativeButton(R.string.label_having_trouble, () -> {
                //todo correct url
                })
                .build(getActivity())
                .show();
    }

    public void showLightToggleButton(final boolean checked) {
        this.expansionLightsToggle.setOnCheckedChangeListener(null);
        this.expansionLightsValue.setVisibility(View.GONE);
        this.expansionLightsToggle.setVisibility(View.VISIBLE);
        this.expansionLightsToggle.setChecked(checked);
    }

    private void updateAlarmBasedOnLightToggleButton() {
        if(this.expansionLightsToggle.isChecked() && this.expansionLightsToggle.isEnabled()) {
            this.alarm.setCategory(Category.LIGHT);
            if(expansionsInteractor.expansions.hasValue()) {
                //todo if there are multiple light expansions integrated how to pick?
                //this.alarm.setExpansions();
            }
        } else {
            this.alarm.setCategory(Category.UNKNOWN);
            this.alarm.setExpansions(new ArrayList<>(0));
        }
    }

    public void hideLightToggleButton(@StringRes final int state) {
        this.expansionLightsValue.setVisibility(View.VISIBLE);
        this.expansionLightsToggle.setVisibility(View.GONE);
        this.expansionLightsValue.setText(state);
    }

    //end region

    public void saveAlarm() {
        if (!dirty && alarm.isEnabled()) {
            finish();
            return;
        }

        alarm.setEnabled(true);

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
                                           Analytics.Backside.PROP_ALARM_DAYS_REPEATED, daysRepeated,
                                           Analytics.Backside.PROP_ALARM_HOUR, alarm.getHourOfDay(),
                                           Analytics.Backside.PROP_ALARM_MINUTE, alarm.getMinuteOfHour());
        Analytics.trackEvent(Analytics.Backside.EVENT_ALARM_SAVED, properties);

        LoadingDialogFragment.close(getFragmentManager());
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    public void presentError(@NonNull final Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getActivity());
        if (e instanceof SmartAlarmInteractor.DayOverlapError) {
            errorDialogBuilder.withMessage(StringRef.from(R.string.error_smart_alarm_day_overlap));
        } else if (ApiException.statusEquals(e, 412)) {
            errorDialogBuilder.withMessage(StringRef.from(getString(R.string.error_smart_alarm_requires_device)));
        }
        final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }
}
