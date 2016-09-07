package is.hello.sense.ui.activities;

import javax.inject.Inject;

import is.hello.sense.SenseApplication;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.ui.common.FragmentNavigationActivity;

/**
 * A fragment navigation activity intended to contain fragments that reference
 * the hardware presenter. The activity will automatically clear any connected
 * peripherals when it is destroyed by the user pressing back.
 */
public class HardwareFragmentActivity extends FragmentNavigationActivity {
    @Inject
    HardwareInteractor hardwarePresenter;

    public HardwareFragmentActivity() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            hardwarePresenter.reset();
        }
    }
}
