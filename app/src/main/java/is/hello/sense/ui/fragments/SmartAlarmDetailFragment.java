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

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ChooseSoundDialogFragment;
import is.hello.sense.ui.dialogs.TimePickerDialogFragment;
import is.hello.sense.util.DateFormatter;

public class SmartAlarmDetailFragment extends InjectionFragment {
    public static final int RESULT_DELETE = 0xD3;

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

    public static final String ARG_ALARM = SmartAlarmDetailFragment.class.getName() + ".ARG_ALARM";
    public static final String ARG_INDEX = SmartAlarmDetailFragment.class.getName() + ".ARG_INDEX";

    public static final int INDEX_NEW = -1;

    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;
    private SmartAlarm smartAlarm;
    private int index = INDEX_NEW;
    private boolean use24Time = false;

    private TextView time;
    private Button soundButton;

    public static @NonNull SmartAlarmDetailFragment newInstance(@NonNull SmartAlarm smartAlarm, int index) {
        SmartAlarmDetailFragment detailFragment = new SmartAlarmDetailFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_ALARM, smartAlarm);
        arguments.putInt(ARG_INDEX, index);
        detailFragment.setArguments(arguments);

        return detailFragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            this.smartAlarm = (SmartAlarm) getArguments().getSerializable(ARG_ALARM);
        } else {
            this.smartAlarm = (SmartAlarm) savedInstanceState.getSerializable(ARG_ALARM);
        }
        this.index = getArguments().getInt(ARG_INDEX);

        if (smartAlarm.getSound() == null) {
            smartAlarm.setSound(SmartAlarm.Sound.none());
        }

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_smart_alarm_detail, container, false);

        this.time = (TextView) view.findViewById(R.id.fragment_smart_alarm_detail_time);
        time.setOnClickListener(this::selectNewTime);
        updateTime();

        ViewGroup repeatDays = (ViewGroup) view.findViewById(R.id.fragment_smart_alarm_detail_repeat);
        View.OnClickListener dayClickListener = this::dayButtonClicked;
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
        soundButton.setOnClickListener(this::selectSound);

        Button deleteButton = (Button) view.findViewById(R.id.fragment_smart_alarm_detail_delete);
        deleteButton.setOnClickListener(this::deleteAlarm);

        if (this.index == INDEX_NEW) {
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
            long selectedId = data.getLongExtra(ChooseSoundDialogFragment.ARG_SELECTED_ID, 0);
            List<SmartAlarm.Sound> sounds = SmartAlarm.Sound.testSounds();
            SmartAlarm.Sound selectedSound = Lists.findFirst(sounds, s -> s.id == selectedId);
            if (selectedSound == null) {
                selectedSound = SmartAlarm.Sound.none();
            }

            smartAlarm.setSound(selectedSound);
            soundButton.setText(selectedSound.name);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_ALARM, smartAlarm);
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

        if (dayButton.isChecked())
            smartAlarm.getDaysOfWeek().add(day);
        else
            smartAlarm.getDaysOfWeek().remove(day);
    }

    public void selectSound(@NonNull View sender) {
        long selectedId = smartAlarm.getSound() != null ? smartAlarm.getSound().id : 0;
        ChooseSoundDialogFragment dialogFragment = ChooseSoundDialogFragment.newInstance(selectedId);
        dialogFragment.setTargetFragment(this, SOUND_REQUEST_CODE);
        dialogFragment.show(getFragmentManager(), ChooseSoundDialogFragment.TAG);
    }


    public void deleteAlarm(@NonNull View sender) {
        Intent response = new Intent();
        response.putExtra(ARG_INDEX, index);
        popFromBackStack(RESULT_DELETE, response);
    }

    public void saveAlarm() {
        Intent response = new Intent();
        response.putExtra(ARG_INDEX, index);
        response.putExtra(ARG_ALARM, smartAlarm);
        popFromBackStack(Activity.RESULT_OK, response);
    }
}
