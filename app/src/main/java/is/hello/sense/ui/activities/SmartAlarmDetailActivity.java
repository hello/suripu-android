package is.hello.sense.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Logger;

public class SmartAlarmDetailActivity extends SenseActivity {
    //region Constants

    public static final String EXTRA_ALARM = SmartAlarmDetailActivity.class.getName() + ".ARG_ALARM";
    public static final String EXTRA_INDEX = SmartAlarmDetailActivity.class.getName() + ".ARG_INDEX";

    public static final int INDEX_NEW = -1;

    //endregion

    public static Bundle getArguments(@NonNull Alarm alarm, int index) {
        Bundle arguments = new Bundle();
        arguments.putSerializable(EXTRA_ALARM, alarm);
        arguments.putInt(EXTRA_INDEX, index);
        return arguments;
    }


    private Alarm alarm;
    private int index;
    private boolean skipUI = false;
    private SmartAlarmDetailFragment detailFragment;


    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            if (AlarmClock.ACTION_SET_ALARM.equals(getIntent().getAction())) {
                processSetAlarmIntent();
            } else {
                this.alarm = (Alarm) getIntent().getSerializableExtra(SmartAlarmDetailActivity.EXTRA_ALARM);
                this.index = getIntent().getIntExtra(SmartAlarmDetailActivity.EXTRA_INDEX, SmartAlarmDetailActivity.INDEX_NEW);
            }
        } else {
            this.alarm = (Alarm) savedInstanceState.getSerializable(SmartAlarmDetailActivity.EXTRA_ALARM);
            this.index = savedInstanceState.getInt("index", INDEX_NEW);
            this.skipUI = savedInstanceState.getBoolean("skipUI", false);
        }

        if (alarm == null) {
            this.alarm = new Alarm();
        }

        setContentView(R.layout.activity_smart_alarm_detail);

        this.detailFragment = (SmartAlarmDetailFragment) getFragmentManager().findFragmentById(R.id.activity_smart_alarm_detail_fragment);

        //noinspection ConstantConditions
        getActionBar().setHomeAsUpIndicator(R.drawable.app_style_ab_cancel);
        getActionBar().setHomeActionContentDescription(android.R.string.cancel);
        getActionBar().setTitle(R.string.title_alarm);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(SmartAlarmDetailActivity.EXTRA_ALARM, alarm);
        outState.putInt("index", index);
        outState.putBoolean("skipUI", skipUI);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.alarm_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.item_save) {
            detailFragment.saveAlarm();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    //endregion


    @Override
    public void onBackPressed() {
        if (detailFragment.isDirty()) {
            SenseAlertDialog backConfirmation = new SenseAlertDialog(this);
            backConfirmation.setTitle(R.string.dialog_title_smart_alarm_edit_cancel);
            backConfirmation.setMessage(R.string.dialog_message_smart_alarm_edit_cancel);
            backConfirmation.setPositiveButton(R.string.action_exit, (dialog, which) -> super.onBackPressed());
            backConfirmation.setNegativeButton(R.string.action_continue, null);
            backConfirmation.setDestructive(true);
            backConfirmation.show();
        } else {
            super.onBackPressed();
        }
    }


    //region Properties

    public @NonNull Alarm getAlarm() {
        return alarm;
    }

    public int getIndex() {
        return index;
    }

    public boolean skipUI() {
        return skipUI;
    }

    //endregion


    //region Set Alarm Intents

    private void processSetAlarmIntent() {
        Intent intent = getIntent();
        int hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 6) + 1;
        int minute = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 30);
        List<Integer> calendarDays;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            calendarDays = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);
        } else {
            calendarDays = Collections.emptyList();
        }

        this.alarm = new Alarm();
        alarm.setTime(new LocalTime(hour, minute));
        if (!Lists.isEmpty(calendarDays)) {
            Set<Integer> days = alarm.getDaysOfWeek();
            for (Integer calendarDay : calendarDays) {
                days.add(calendarDayToDateTimeDay(calendarDay));
            }
        }

        this.index = SmartAlarmDetailActivity.INDEX_NEW;
        this.skipUI = (intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false) &&
                       intent.hasExtra(AlarmClock.EXTRA_HOUR) &&
                       intent.hasExtra(AlarmClock.EXTRA_MINUTES));
    }

    private int calendarDayToDateTimeDay(int calendarDay) {
        switch (calendarDay) {
            case Calendar.SUNDAY: {
                return DateTimeConstants.SUNDAY;
            }

            case Calendar.MONDAY: {
                return DateTimeConstants.MONDAY;
            }

            case Calendar.TUESDAY: {
                return DateTimeConstants.TUESDAY;
            }

            case Calendar.WEDNESDAY: {
                return DateTimeConstants.WEDNESDAY;
            }

            case Calendar.THURSDAY: {
                return DateTimeConstants.THURSDAY;
            }

            case Calendar.FRIDAY: {
                return DateTimeConstants.FRIDAY;
            }

            case Calendar.SATURDAY: {
                return DateTimeConstants.SATURDAY;
            }

            default: {
                Logger.warn(getClass().getSimpleName(), "Unknown calendar day " + calendarDay);
                return DateTimeConstants.MONDAY;
            }
        }
    }

    //endregion
}
