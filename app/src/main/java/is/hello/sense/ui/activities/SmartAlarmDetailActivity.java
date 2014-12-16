package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;

import is.hello.sense.R;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;

public class SmartAlarmDetailActivity extends SenseActivity {
    public static final int RESULT_DELETE = 0xD3;

    public static final String EXTRA_ALARM = SmartAlarmDetailActivity.class.getName() + ".ARG_ALARM";
    public static final String EXTRA_INDEX = SmartAlarmDetailActivity.class.getName() + ".ARG_INDEX";

    public static final int INDEX_NEW = -1;

    public static Bundle getArguments(@NonNull SmartAlarm smartAlarm, int index) {
        Bundle arguments = new Bundle();
        arguments.putSerializable(EXTRA_ALARM, smartAlarm);
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
        getActionBar().setHomeAsUpIndicator(R.drawable.app_style_ab_done);
        SpannableString titleString = new SpannableString(getString(R.string.action_done));
        titleString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_accent)),
                                                    0, titleString.length(),
                                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        getActionBar().setTitle(titleString);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            detailFragment.saveAlarm();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }


    @Override
    public void onBackPressed() {
        SenseAlertDialog backConfirmation = new SenseAlertDialog(this);
        backConfirmation.setTitle(R.string.dialog_title_smart_alarm_edit_cancel);
        backConfirmation.setMessage(R.string.dialog_message_smart_alarm_edit_cancel);
        backConfirmation.setPositiveButton(R.string.action_exit, (dialog, which) -> super.onBackPressed());
        backConfirmation.setNegativeButton(R.string.action_continue, null);
        backConfirmation.setDestructive(true);
        backConfirmation.show();
    }
}
