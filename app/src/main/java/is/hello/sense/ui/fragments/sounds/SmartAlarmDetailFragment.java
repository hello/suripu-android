package is.hello.sense.ui.fragments.sounds;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import com.segment.analytics.Properties;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.ExpansionAlarm;
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.flows.expansions.interactors.ExpansionsInteractor;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.expansions.ui.activities.ExpansionValuePickerActivity;
import is.hello.sense.flows.expansions.utils.ExpansionCategoryFormatter;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SmartAlarmInteractor;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.adapter.ExpansionAlarmsAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.TimePickerDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.DividerItemDecoration;
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
    private static final int EXPANSION_VALUE_REQUEST_CODE = 0x69;

    @Inject
    DateFormatter dateFormatter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    SmartAlarmInteractor smartAlarmInteractor;
    @Inject
    ExpansionsInteractor expansionsInteractor;
    @Inject
    ExpansionCategoryFormatter expansionCategoryFormatter;

    private boolean dirty = false;
    private boolean wantsTone = false;

    private Alarm alarm;
    private int index = SmartAlarmDetailActivity.INDEX_NEW;
    private boolean use24Time = false;

    private TextView time;
    private TextView toneName;
    private TextView repeatDays;
    private RecyclerView expansionAlarmsRecyclerView;
    private ExpansionAlarmsAdapter expansionAlarmsAdapter;


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

        smartAlarmInteractor.update();
        addPresenter(smartAlarmInteractor);
        addPresenter(preferences);
        addPresenter(expansionsInteractor);

        setRetainInstance(true);

        expansionAlarmsAdapter = new ExpansionAlarmsAdapter(new ArrayList<>(2));
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

        expansionAlarmsRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_smart_alarm_detail_expansion_rv);
        expansionAlarmsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        expansionAlarmsRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
        expansionAlarmsRecyclerView.setNestedScrollingEnabled(false);
        expansionAlarmsRecyclerView.setHasFixedSize(false);
        expansionAlarmsRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        expansionAlarmsRecyclerView.setAdapter(expansionAlarmsAdapter);
        expansionAlarmsAdapter.setOnItemClickedListener(this::onExpansionAlarmItemClicked);

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
        if (BuildConfig.DEBUG) { //todo add support for prod
            //expansionsInteractor.expansions.forget();
            bindAndSubscribe(expansionsInteractor.findByCategories(Arrays.asList(Category.LIGHT, Category.TEMPERATURE)),
                             this::bindFilteredExpansions,
                             this::bindExpansionsThrowable);

            bindAndSubscribe(preferences.observableBoolean(PreferencesInteractor.HAS_VOICE, false),
                             enabled -> {
                                 if (enabled) {
                                     expansionAlarmsRecyclerView.setVisibility(View.VISIBLE);
                                     expansionsInteractor.update();
                                 } else {
                                     expansionAlarmsRecyclerView.setVisibility(View.GONE);
                                     expansionsInteractor.expansions.forget();
                                 }
                             },
                             Functions.LOG_ERROR);

        }
        bindAndSubscribe(preferences.observableUse24Time(),
                         newValue -> {
                             this.use24Time = newValue;
                             updateTime();
                         },
                         Functions.LOG_ERROR);

        bindAndSubscribe(smartAlarmInteractor.availableAlarmSounds(),
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

        if (getDetailActivity().skipUI()) {
            LoadingDialogFragment.show(getFragmentManager(),
                                       null, LoadingDialogFragment.DEFAULTS);
        } else {
            WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_alarm, false);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            attemptToUpdateExpansions();
            return;
        }

        if (data == null) {
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
        } else if (requestCode == EXPANSION_VALUE_REQUEST_CODE) {
            final ExpansionAlarm expansionAlarm = (ExpansionAlarm) data.getSerializableExtra(ExpansionValuePickerActivity.EXTRA_EXPANSION_ALARM);
            updateExpansionAlarmFromResult(expansionAlarm);
        }
    }

    private void updateExpansionAlarmFromResult(@Nullable final ExpansionAlarm expansionAlarm) {
        if(expansionAlarm == null){
            return;
        }
        //todo ExpansionDetailFragment needs to
        if(expansionAlarm.isEnabled() && expansionAlarm.hasExpansionRange()) {
            expansionAlarm.setDisplayValue(expansionCategoryFormatter.getFormattedValueRange(expansionAlarm.getCategory(), expansionAlarm.getExpansionRange(), getActivity()));
        } else {
            expansionAlarm.setDisplayValue(getString(expansionCategoryFormatter.getDisplayValueRes(expansionAlarm.isEnabled())));
        }
        expansionAlarm.setDisplayIcon(expansionCategoryFormatter.getDisplayIconRes(expansionAlarm.getCategory()));
        expansionAlarmsAdapter.updateLastClickedItem(expansionAlarm);
        alarm.setExpansions(expansionAlarmsAdapter.getAllEnabledWithValueRangeCopy());
        markDirty();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("dirty", dirty);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (expansionAlarmsAdapter != null) {
            expansionAlarmsAdapter.setOnItemClickedListener(null);
        }
        if (expansionAlarmsRecyclerView != null) {
            expansionAlarmsRecyclerView.setAdapter(null);
            expansionAlarmsRecyclerView = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        smartAlarmInteractor.forgetAvailableAlarmSoundsCache();

        expansionAlarmsAdapter = null;
    }


    private SmartAlarmDetailActivity getDetailActivity() {
        return (SmartAlarmDetailActivity) getActivity();
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
            smartAlarmInteractor.update();
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
            bindAndSubscribe(smartAlarmInteractor.deleteSmartAlarm(index),
                             ignored -> finish(),
                             this::presentError);
        });
        confirmDelete.setNegativeButton(android.R.string.cancel, null);
        confirmDelete.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        confirmDelete.show();
    }

    //region Expansion

    public void bindFilteredExpansions(@NonNull final List<Expansion> expansions) {

        final List<ExpansionAlarm> expansionAlarms = new ArrayList<>(expansions.size());

        for (int i = 0; i < expansions.size(); i++) {
            final Expansion expansion = expansions.get(i);
            final ExpansionAlarm existing = alarm.getExpansion(i);
            final ExpansionAlarm updatedExpansionAlarm = new ExpansionAlarm(expansion);
            if (existing != null
                    && existing.hasExpansionRange()
                    && updatedExpansionAlarm.isEnabled()) {
                updatedExpansionAlarm.setExpansionRange(existing.getExpansionRange());
                updatedExpansionAlarm.setDisplayValue(expansionCategoryFormatter.getFormattedValueRange(existing.getCategory(),
                                                                                                        existing.getExpansionRange(),
                                                                                                        getActivity())
                                              );

            } else {
                updatedExpansionAlarm.setDisplayValue(getString(expansionCategoryFormatter.getDisplayValueResFromState(expansion.getState())));
            }

            updatedExpansionAlarm.setDisplayIcon(expansionCategoryFormatter.getDisplayIconRes(expansion.getCategory()));

            expansionAlarms.add(updatedExpansionAlarm);
        }
        expansionAlarmsAdapter.replaceAll(expansionAlarms);
        alarm.setExpansions(expansionAlarms);
    }

    private void onExpansionAlarmItemClicked(final int position, @NonNull final ExpansionAlarm expansionAlarm) {
        if (expansionAlarm.isEnabled()) {
            redirectToExpansionPicker(expansionAlarm.getId(),
                                      expansionAlarm.getCategory(),
                                      expansionAlarm.getExpansionRange());
        } else {
            redirectToExpansionDetail(expansionAlarm.getId());
        }
    }

    private void redirectToExpansionPicker(final long expansionId,
                                           @NonNull final Category expansionCategory,
                                           @Nullable final ExpansionValueRange valueRange) {
        startActivityForResult(ExpansionValuePickerActivity.getIntent(getActivity(),
                                                                      expansionId,
                                                                      expansionCategory,
                                                                      valueRange),
                               EXPANSION_VALUE_REQUEST_CODE);
    }

    private void redirectToExpansionDetail(final long expansionId) {
        startActivityForResult(ExpansionSettingsActivity.getExpansionDetailIntent(getActivity(),
                                                                                  expansionId),
                               EXPANSION_VALUE_REQUEST_CODE);
    }

    private void bindExpansionsThrowable(@NonNull final Throwable throwable) {
        this.showExpansionError();
    }

    public void onExpansionErrorIconPress(@NonNull final View view) {
        attemptToUpdateExpansions();
    }

    private void showExpansionError() {
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

    private void attemptToUpdateExpansions() {
        if (preferences.hasVoice() && BuildConfig.DEBUG) {/// todo add support for prod
            expansionsInteractor.update();
        }
    }

    private void updateAlarmFromAdapterExpansions() {
        this.alarm.setExpansions(expansionAlarmsAdapter.getAllEnabledWithValueRangeCopy());
        finishSaveAlarmOperation();
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
            } else if (smartAlarmInteractor.isAlarmTooSoon(alarm)) {
                LoadingDialogFragment.close(getFragmentManager());

                final ErrorDialogFragment dialogFragment = new ErrorDialogFragment.Builder()
                        .withMessage(StringRef.from(R.string.error_alarm_too_soon))
                        .build();
                dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            } else {
                if (alarm.getDaysOfWeek().isEmpty()) {
                    alarm.setRingOnce();
                }
                updateAlarmFromAdapterExpansions();
            }
        });
    }

    private void finishSaveAlarmOperation() {
        final Observable<VoidResponse> saveOperation;
        if (index == SmartAlarmDetailActivity.INDEX_NEW) {
            saveOperation = smartAlarmInteractor.addSmartAlarm(alarm);
        } else {
            saveOperation = smartAlarmInteractor.saveSmartAlarm(index, alarm);
        }

        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.DEFAULTS);
        bindAndSubscribe(saveOperation, ignored -> finish(), this::presentError);
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
