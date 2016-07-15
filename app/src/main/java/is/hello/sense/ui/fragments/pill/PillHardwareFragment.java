package is.hello.sense.ui.fragments.pill;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BatteryUtil;

public abstract class PillHardwareFragment extends InjectionFragment {

    @Inject
    DevicesPresenter devicesPresenter;

    private LoadingDialogFragment loadingDialogFragment;

    private LocationPermission locationPermission;

    public static BatteryUtil.Operation pillUpdateOperationNoCharge() {
        return new BatteryUtil.Operation(0.20, false);
    }

    public static BatteryUtil.Operation pillUpdateOperationWithCharge(){
        return new BatteryUtil.Operation(0, true);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationPermission = new LocationPermission(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationPermission = null;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull  final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(locationPermission.isGrantedFromResult(requestCode, permissions, grantResults)){
            onLocationPermissionGranted(true);
        } else {
            locationPermission.showEnableInstructionsDialog();
        }
    }

    protected void requestLocationPermission() {
        locationPermission.requestPermissionWithDialog();
    }

    protected boolean isLocationPermissionGranted(){
        return locationPermission.isGranted();
    }

    abstract void onLocationPermissionGranted(final boolean isGranted);

    protected void showBlockingActivity(@StringRes final int titleRes) {
        if (loadingDialogFragment == null) {
            stateSafeExecutor.execute(() -> this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                                                                                                getString(titleRes),
                                                                                                LoadingDialogFragment.OPAQUE_BACKGROUND));
        } else {
            if(loadingDialogFragment.getDialog() == null){
                loadingDialogFragment.showAllowingStateLoss(getFragmentManager(),LoadingDialogFragment.TAG);
            }
            loadingDialogFragment.setTitle(getString(titleRes));
        }
    }

    protected void hideBlockingActivity(final boolean success, @NonNull final Runnable onCompletion) {
        stateSafeExecutor.execute(() -> {
            if (success) {
                LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                    this.loadingDialogFragment = null;
                    stateSafeExecutor.execute(onCompletion);
                });
            } else {
                LoadingDialogFragment.close(getFragmentManager());
                this.loadingDialogFragment = null;
                onCompletion.run();
            }
        });
    }

    protected void hideBlockingActivity(){
        //for delay of 1000 millisecond
        LoadingDialogFragment.closeWithOnComplete(getFragmentManager(), null);
    }

    protected void presentPillBatteryError(){
        final String helpUriString = UserSupport.DeviceIssue.SLEEP_PILL_LOW_BATTERY.getUri().toString();
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                .withOperation("Check Pill Battery")
                .withTitle(R.string.dialog_title_replace_sleep_pill_battery)
                .withMessage(StringRef.from(R.string.dialog_message_replace_sleep_pill_battery))
                .withAction(helpUriString, R.string.label_having_trouble)
                .withContextInfo(Analytics.PillUpdate.Error.PILL_REPLACE_BATTERY)
                .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    protected void presentPhoneBatteryError(){
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                .withOperation("Check Phone Battery")
                .withTitle(R.string.error_phone_battery_low_title)
                .withMessage(StringRef.from(R.string.error_phone_battery_low_message))
                .withContextInfo(Analytics.PillUpdate.Error.PHONE_BATTERY_LOW)
                .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

}
