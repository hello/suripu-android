package is.hello.sense.ui.activities;

import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.SmartAlarmListFragment;

public class SmartAlarmActivity extends FragmentNavigationActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            showFragment(new SmartAlarmListFragment(), getString(R.string.action_alarm), false);
        }
    }

    @Override
    protected int getDefaultTitle() {
        return R.string.action_alarm;
    }
}
