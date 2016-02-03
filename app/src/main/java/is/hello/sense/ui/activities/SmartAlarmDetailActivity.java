package is.hello.sense.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import com.segment.analytics.Properties;

import org.joda.time.LocalTime;

import java.util.List;
import java.util.Set;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;

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
                Properties properties = Analytics.createProperties(
                    Analytics.Global.PROP_ALARM_CLOCK_INTENT_NAME, "ACTION_SET_ALARM"
                );
                Analytics.trackEvent(Analytics.Global.EVENT_ALARM_CLOCK_INTENT, properties);
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
            if (index == INDEX_NEW) {
                backConfirmation.setTitle(R.string.dialog_title_smart_alarm_new_cancel);
                backConfirmation.setMessage(R.string.dialog_message_smart_alarm_new_cancel);
            } else {
                backConfirmation.setTitle(R.string.dialog_title_smart_alarm_edit_cancel);
                backConfirmation.setMessage(R.string.dialog_message_smart_alarm_edit_cancel);
            }
            backConfirmation.setNegativeButton(R.string.action_keep_editing, null);
            backConfirmation.setPositiveButton(R.string.action_discard, (dialog, which) -> super.onBackPressed());
            backConfirmation.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
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
        final Intent intent = getIntent();
        final int hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 6) + 1;
        final int minute = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 30);
        final List<Integer> calendarDays = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS);

        this.alarm = new Alarm();
        alarm.setTime(new LocalTime(hour, minute));
        if (!Lists.isEmpty(calendarDays)) {
            final Set<Integer> days = alarm.getDaysOfWeek();
            for (final Integer calendarDay : calendarDays) {
                days.add(DateFormatter.calendarDayToJodaTimeDay(calendarDay));
            }
        }

        this.index = SmartAlarmDetailActivity.INDEX_NEW;
        this.skipUI = (intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false) &&
                       intent.hasExtra(AlarmClock.EXTRA_HOUR) &&
                       intent.hasExtra(AlarmClock.EXTRA_MINUTES));
    }

    //endregion
}
