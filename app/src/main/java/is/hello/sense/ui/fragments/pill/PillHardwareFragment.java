package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BatteryUtil;

/**
 * This class requires the {@link Activity} it's in to implement {@link is.hello.sense.ui.common.FragmentNavigation}.
 */
public abstract class PillHardwareFragment extends InjectionFragment {

    private final LocationPermission locationPermission = new LocationPermission(this);
    protected OnboardingToolbar toolbar;

    public static BatteryUtil.Operation pillUpdateOperationNoCharge() {
        return new BatteryUtil.Operation(0.20, false);
    }

    public static BatteryUtil.Operation pillUpdateOperationWithCharge() {
        return new BatteryUtil.Operation(0, true);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getFragmentNavigation() == null){
            Log.d(getTag(), "onCreate: activity must implement " + FragmentNavigation.class);
            finishWithResult(Activity.RESULT_CANCELED, null);
            return;
        }
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

    protected void cancel(final boolean needsBle) {
        final Intent intent = new Intent();
        intent.putExtra(PillUpdateActivity.ARG_NEEDS_BLUETOOTH, needsBle);
        getFragmentNavigation().flowFinished(this, Activity.RESULT_CANCELED, intent);
    }
}
