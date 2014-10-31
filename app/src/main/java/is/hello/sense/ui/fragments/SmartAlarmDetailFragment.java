package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.joda.time.LocalTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ChooseSoundDialogFragment;
import is.hello.sense.ui.dialogs.TimePickerDialogFragment;
import is.hello.sense.util.DateFormatter;

public class SmartAlarmDetailFragment extends InjectionFragment {
    public static final int RESULT_DELETE = 0xD3;

    private static final int TIME_REQUEST_CODE = 0x747;
    public static final String ARG_ALARM = SmartAlarmDetailFragment.class.getName() + ".ARG_ALARM";
    public static final String ARG_INDEX = SmartAlarmDetailFragment.class.getName() + ".ARG_INDEX";

    public static final int INDEX_NEW = -1;

    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;
    private SmartAlarm smartAlarm;
    private int index = INDEX_NEW;
    private boolean use24Time = false;

    private TextView time;

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

        this.smartAlarm = (SmartAlarm) getArguments().getSerializable(ARG_ALARM);
        this.index = getArguments().getInt(ARG_INDEX);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_smart_alarm_detail, container, false);

        this.time = (TextView) view.findViewById(R.id.fragment_smart_alarm_detail_time);
        time.setOnClickListener(this::selectNewTime);
        updateTime();

        ViewGroup repeat = (ViewGroup) view.findViewById(R.id.fragment_smart_alarm_detail_repeat);
        View.OnClickListener dayClickListener = this::dayButtonClicked;
        for (int i = 0, count = repeat.getChildCount(); i < count; i++) {
            int day = i + 1;
            ToggleButton dayButton = (ToggleButton) repeat.getChildAt(i);
            dayButton.setTag(day);
            dayButton.setOnClickListener(dayClickListener);
            dayButton.setChecked(smartAlarm.getDaysOfWeek().contains(day));
        }

        ToggleButton enabled = (ToggleButton) view.findViewById(R.id.fragment_smart_alarm_detail_enabled);
        enabled.setEnabled(smartAlarm.isEnabled());
        enabled.setOnCheckedChangeListener((button, isEnabled) -> smartAlarm.setEnabled(isEnabled));

        Button sound = (Button) view.findViewById(R.id.fragment_smart_alarm_detail_sound);
        sound.setOnClickListener(this::selectSound);

        Button delete = (Button) view.findViewById(R.id.fragment_smart_alarm_detail_delete);
        delete.setOnClickListener(this::deleteAlarm);

        Button save = (Button) view.findViewById(R.id.fragment_smart_alarm_detail_save);
        save.setOnClickListener(this::saveAlarm);

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
        }
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
        // TODO: Get those sounds in here.
        ChooseSoundDialogFragment dialogFragment = new ChooseSoundDialogFragment();
        dialogFragment.show(getFragmentManager(), ChooseSoundDialogFragment.TAG);
    }


    public void deleteAlarm(@NonNull View sender) {
        if (getTargetFragment() != null) {
            Intent response = new Intent();
            response.putExtra(ARG_INDEX, index);
            getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_DELETE, response);
        }
        getFragmentManager().popBackStackImmediate();
    }

    public void saveAlarm(@NonNull View sender) {
        if (getTargetFragment() != null) {
            Intent response = new Intent();
            response.putExtra(ARG_INDEX, index);
            response.putExtra(ARG_ALARM, smartAlarm);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
        }
        getFragmentManager().popBackStackImmediate();
    }
}
