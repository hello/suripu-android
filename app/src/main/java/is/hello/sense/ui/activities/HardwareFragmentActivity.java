package is.hello.sense.ui.activities;

import javax.inject.Inject;

import is.hello.commonsense.service.SenseService;
import is.hello.commonsense.service.SenseServiceConnection;
import is.hello.sense.SenseApplication;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.common.FragmentNavigationActivity;

/**
 * A fragment navigation activity intended to contain fragments that reference
 * the hardware presenter. The activity will automatically clear any connected
 * peripherals when it is destroyed by the user pressing back.
 */
public class HardwareFragmentActivity extends FragmentNavigationActivity {
    @Inject SenseServiceConnection serviceConnection;

    public HardwareFragmentActivity() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            serviceConnection.senseService()
                             .filter(SenseService::isConnected)
                             .flatMap(SenseService::disconnect)
                             .subscribe(Functions.NO_OP,
                                        Functions.LOG_ERROR);
        }
    }
}
