package is.hello.sense.flows.nightmode.ui.activities;

import android.content.Intent;

import is.hello.sense.flows.nightmode.ui.fragments.NightModeFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;

public class NightModeActivity extends FragmentNavigationActivity {

    public static final int REQUEST_LOCATION_STATUS = 921;

    @Override

    protected void onCreateAction() {
        navigationDelegate.pushFragment(new NightModeFragment(),
                                        null,
                                        false);
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode,
                                    final Intent data) {
        if (requestCode != REQUEST_LOCATION_STATUS) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (getTopFragment() == null) {
            return;
        }
        getTopFragment().onActivityResult(requestCode, resultCode, data);
    }
}
