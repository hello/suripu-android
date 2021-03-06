package is.hello.sense.permissions;


import android.Manifest;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.ui.common.UserSupport;

public class LocationPermission extends Permission {
    public LocationPermission(@NonNull final Fragment fragment) {
        super(fragment);
    }

    public LocationPermission(@NonNull Fragment fragment,
                              @StringRes int negativeButtonText,
                              @StringRes int positiveButtonText) {
        super(fragment, negativeButtonText, positiveButtonText);
    }

    @Override
    protected String getPermissionName() {
        return Manifest.permission.ACCESS_COARSE_LOCATION;
    }

    @Override
    public void requestPermissionWithDialog() {
        requestPermissionWithDialog(
                R.string.request_permission_location_title,
                R.string.request_permission_location_message,
                (dialog, which) -> UserSupport.showLocationPermissionMoreInfoPage(fragment.getActivity()));
    }

    @Override
    public void showEnableInstructionsDialog() {
        showEnableInstructionsDialog(R.string.request_permission_location_required_title,
                                     R.string.request_permission_location_required_message,
                                     (dialog, which) -> UserSupport.showLocationPermissionMoreInfoPage(fragment.getActivity()));
    }
}
