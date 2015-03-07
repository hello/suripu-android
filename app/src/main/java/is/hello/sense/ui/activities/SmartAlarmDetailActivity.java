package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;

public class SmartAlarmDetailActivity extends SenseActivity {
    public static final String EXTRA_ALARM = SmartAlarmDetailActivity.class.getName() + ".ARG_ALARM";
    public static final String EXTRA_INDEX = SmartAlarmDetailActivity.class.getName() + ".ARG_INDEX";

    public static final int INDEX_NEW = -1;

    public static Bundle getArguments(@NonNull Alarm alarm, int index) {
        Bundle arguments = new Bundle();
        arguments.putSerializable(EXTRA_ALARM, alarm);
        arguments.putInt(EXTRA_INDEX, index);
        return arguments;
    }


    private SmartAlarmDetailFragment detailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_alarm_detail);

        this.detailFragment = (SmartAlarmDetailFragment) getFragmentManager().findFragmentById(R.id.activity_smart_alarm_detail_fragment);

        //noinspection ConstantConditions
        getActionBar().setHomeAsUpIndicator(R.drawable.app_style_ab_cancel);
        getActionBar().setTitle(R.string.title_alarm);
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
}
