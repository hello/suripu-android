package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.activities.SmartAlarmDetailActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.SmartAlarmSoundDialogFragment;
import is.hello.sense.ui.dialogs.TimePickerDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.SafeOnClickListener;
import rx.Observable;

public class SmartAlarmDetailFragment extends InjectionFragment {
    private static final int TIME_REQUEST_CODE = 0x747;
    private static final int[] DAY_TAGS = {
            DateTimeConstants.SUNDAY,
            DateTimeConstants.MONDAY,
            DateTimeConstants.TUESDAY,
            DateTimeConstants.WEDNESDAY,
            DateTimeConstants.THURSDAY,
            DateTimeConstants.FRIDAY,
            DateTimeConstants.SATURDAY,
    };

    private static final int SOUND_REQUEST_CODE = 0x50;

    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;
    @Inject SmartAlarmPresenter smartAlarmPresenter;

    private SmartAlarm smartAlarm;
    private int index = SmartAlarmDetailActivity.INDEX_NEW;
    private boolean use24Time = false;

    private TextView time;
    private Button soundButton;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            this.smartAlarm = (SmartAlarm) getActivity().getIntent().getSerializableExtra(SmartAlarmDetailActivity.EXTRA_ALARM);
        } else {
            this.smartAlarm = (SmartAlarm) savedInstanceState.getSerializable(SmartAlarmDetailActivity.EXTRA_ALARM);
        }

        if (smartAlarm == null) {
            this.smartAlarm = new SmartAlarm();
        }

        this.index = getActivity().getIntent().getIntExtra(SmartAlarmDetailActivity.EXTRA_INDEX, SmartAlarmDetailActivity.INDEX_NEW);

        smartAlarmPresenter.update();
        addPresenter(smartAlarmPresenter);
        addPresenter(preferences);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_smart_alarm_detail, container, false);

        this.time = (TextView) view.findViewById(R.id.fragment_smart_alarm_detail_time);
        Views.setSafeOnClickListener(time, this::selectNewTime);
        updateTime();

        ViewGroup repeatDays = (ViewGroup) view.findViewById(R.id.fragment_smart_alarm_detail_repeat);
        View.OnClickListener dayClickListener = new SafeOnClickListener(this::dayButtonClicked);
        for (int i = 0, count = repeatDays.getChildCount(); i < count; i++) {
            int day = DAY_TAGS[i];
            ToggleButton dayButton = (ToggleButton) repeatDays.getChildAt(i);
            dayButton.setOnClickListener(dayClickListener);
            dayButton.setChecked(smartAlarm.getDaysOfWeek().contains(day));
            dayButton.setTag(day);
        }

        ToggleButton enabledButton = (ToggleButton) view.findViewById(R.id.fragment_smart_alarm_detail_enabled);
        enabledButton.setChecked(smartAlarm.isEnabled());
        enabledButton.setOnCheckedChangeListener((button, isEnabled) -> smartAlarm.setEnabled(isEnabled));

        this.soundButton = (Button) view.findViewById(R.id.fragment_smart_alarm_detail_sound);
        if (smartAlarm.getSound() != null && !TextUtils.isEmpty(smartAlarm.getSound().name)) {
            soundButton.setText(smartAlarm.getSound().name);
        } else {
            soundButton.setText(R.string.no_sound_placeholder);
        }
        Views.setSafeOnClickListener(soundButton, this::selectSound);

        Button deleteButton = (Button) view.findViewById(R.id.fragment_smart_alarm_detail_delete);
        Views.setSafeOnClickListener(deleteButton, this::deleteAlarm);

        if (this.index == SmartAlarmDetailActivity.INDEX_NEW) {
            view.findViewById(R.id.fragment_smart_alarm_detail_delete_container).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(preferences.observableBoolean(PreferencesPresenter.USE_24_TIME, false), newValue -> {
            this.use24Time = newValue;
            updateTime();
        }, Functions.LOG_ERROR);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TIME_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            int hour = data.getIntExtra(TimePickerDialogFragment.RESULT_HOUR, 7);
            int minute = data.getIntExtra(TimePickerDialogFragment.RESULT_MINUTE, 30);
            smartAlarm.setTime(new LocalTime(hour, minute));
            updateTime();
        } else if (requestCode == SOUND_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            SmartAlarm.Sound selectedSound = (SmartAlarm.Sound) data.getSerializableExtra(SmartAlarmSoundDialogFragment.ARG_SELECTED_SOUND);
            smartAlarm.setSound(selectedSound);
            soundButton.setText(selectedSound.name);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(SmartAlarmDetailActivity.EXTRA_ALARM, smartAlarm);
    }

    public void updateTime() {
        String formattedTime = dateFormatter.formatAsTime(smartAlarm.getTime(), use24Time);
        time.setText(formattedTime);
    }

    public void selectNewTime(@NonNull View sender) {
        TimePickerDialogFragment dialogFragment = TimePickerDialogFragment.newInstance(smartAlarm.getTime());
        dialogFragment.setTargetFragment(this, TIME_REQUEST_CODE);
        dialogFragment.show(getFragmentManager(), TimePickerDialogFragment.TAG);
    }

    public void dayButtonClicked(@NonNull View sender) {
        ToggleButton dayButton = (ToggleButton) sender;
        int day = (Integer) dayButton.getTag();

        if (dayButton.isChecked()) {
            smartAlarm.getDaysOfWeek().add(day);
        } else {
            smartAlarm.getDaysOfWeek().remove(day);
        }

        smartAlarm.setRepeated(!smartAlarm.getDaysOfWeek().isEmpty());
    }

    public void selectSound(@NonNull View sender) {
        SmartAlarmSoundDialogFragment dialogFragment = SmartAlarmSoundDialogFragment.newInstance(smartAlarm.getSound());
        dialogFragment.setTargetFragment(this, SOUND_REQUEST_CODE);
        dialogFragment.show(getFragmentManager(), SmartAlarmSoundDialogFragment.TAG);
    }


    public void deleteAlarm(@NonNull View sender) {
        LoadingDialogFragment.show(getFragmentManager(), null, false);
        bindAndSubscribe(smartAlarmPresenter.deleteSmartAlarm(index), ignored -> finish(), this::presentError);
    }

    public void saveAlarm() {
        if (smartAlarm.getSound() == null) {
            ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_no_smart_alarm_sound));
            dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
        } else {
            Observable<VoidResponse> saveOperation;
            if (index == SmartAlarmDetailActivity.INDEX_NEW) {
                saveOperation = smartAlarmPresenter.addSmartAlarm(smartAlarm);
            } else {
                saveOperation = smartAlarmPresenter.saveSmartAlarm(index, smartAlarm);
            }

            LoadingDialogFragment.show(getFragmentManager(), null, false);
            bindAndSubscribe(saveOperation, ignored -> finish(), this::presentError);
        }
    }

    public void finish() {
        LoadingDialogFragment.close(getFragmentManager());
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        if (e instanceof SmartAlarmPresenter.DayOverlapError) {
            ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_smart_alarm_day_overlap));
            dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
        } else {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }
    }
}
