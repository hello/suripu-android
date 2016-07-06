package is.hello.sense.ui.fragments.pill;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.bluetooth.exceptions.PillNotFoundException;
import is.hello.sense.bluetooth.exceptions.RssiException;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BatteryUtil;

public abstract class PillHardwareFragment extends InjectionFragment {

    @Inject
    DevicesPresenter devicesPresenter;

    private LoadingDialogFragment loadingDialogFragment;

    /**
     * @return the name of the operation for the fragments error dialog.
     */
    @StringRes
    abstract String operationString();

    private final LocationPermission locationPermission = new LocationPermission(this);

    public static BatteryUtil.Operation pillUpdateOperationNoCharge() {
        return new BatteryUtil.Operation(0.20, false);
    }

    public static BatteryUtil.Operation pillUpdateOperationWithCharge(){
        return new BatteryUtil.Operation(0, true);
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

    protected void presentPhoneBatteryError(){
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                .withOperation("Check Phone Battery")
                .withTitle(R.string.error_phone_battery_low_title)
                .withMessage(StringRef.from(R.string.error_phone_battery_low_message))
                .withContextInfo(Analytics.PillUpdate.Error.PHONE_BATTERY_LOW)
                .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    public PillUpdateActivity getPillUpdateActivity() {
        return ((PillUpdateActivity) getActivity());
    }

    protected void presentError(final Throwable e) {
        //todo confirm all error states.
        @StringRes int title = R.string.error_sleep_pill_title_update_missing;
        final String helpUriString = UserSupport.DeviceIssue.SLEEP_PILL_WEAK_RSSI.getUri().toString();
        final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getActivity());
        errorDialogBuilder.withOperation(operationString());

        if (e instanceof RssiException) {
            title = R.string.error_pill_too_far;
        } else if (e instanceof PillNotFoundException) {
            title = R.string.error_pill_not_found;
        } else {
            errorDialogBuilder.withUnstableBluetoothHelp(getActivity());
        }
        errorDialogBuilder
                .withTitle(title)
                .withMessage(StringRef.from(R.string.error_sleep_pill_message_update_missing))
                .withAction(helpUriString, R.string.label_having_trouble)
                .build()
                .showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);

    }
}
