package is.hello.sense.flows.nightmode.ui.activities;

import android.content.Intent;

import is.hello.sense.flows.nightmode.ui.fragments.NightModeFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;

public class NightModeActivity extends FragmentNavigationActivity {

    public static final int REQUEST_LOCATION_SETTING_CHANGE = 110;

    @Override
    protected void onCreateAction() {
        navigationDelegate.pushFragment(new NightModeFragment(),
                                        null,
                                        false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCATION_SETTING_CHANGE) {
            // do nothing because fragment should update on resume
        }
    }
}
