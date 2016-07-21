package is.hello.sense.ui.fragments.pill;

import android.support.annotation.NonNull;
import android.view.View;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BatteryUtil;

public abstract class PillHardwareFragment extends InjectionFragment {

    @Inject
    DevicesPresenter devicesPresenter;

    private final LocationPermission locationPermission = new LocationPermission(this);
    protected OnboardingToolbar toolbar;

    public static BatteryUtil.Operation pillUpdateOperationNoCharge() {
        return new BatteryUtil.Operation(0.20, false);
    }

    public static BatteryUtil.Operation pillUpdateOperationWithCharge() {
        return new BatteryUtil.Operation(0, true);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (locationPermission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            onLocationPermissionGranted(true);
        } else {
            locationPermission.showEnableInstructionsDialog();
        }
    }

    protected void requestLocationPermission() {
        locationPermission.requestPermissionWithDialog();
    }

    protected boolean isLocationPermissionGranted() {
        return locationPermission.isGranted();
    }

    abstract void onLocationPermissionGranted(final boolean isGranted);

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (toolbar != null) {
            this.toolbar.onDestroyView();
            this.toolbar = null;
        }
    }

    protected void presentPhoneBatteryError() {
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                .withOperation("Check Phone Battery")
                .withTitle(R.string.error_phone_battery_low_title)
                .withMessage(StringRef.from(R.string.error_phone_battery_low_message))
                .withContextInfo(Analytics.PillUpdate.Error.PHONE_BATTERY_LOW)
                .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    protected void help(final View ignored) {
        UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.UPDATE_PILL);
    }
}
