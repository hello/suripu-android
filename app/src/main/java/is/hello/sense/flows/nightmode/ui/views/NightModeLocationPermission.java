package is.hello.sense.flows.nightmode.ui.views;

import android.app.Fragment;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.common.UserSupport;

public class NightModeLocationPermission extends LocationPermission {

    public NightModeLocationPermission(@NonNull final Fragment fragment) {
        super(fragment);
    }

    @Override
    public void requestPermissionWithDialog() {
        requestPermissionWithDialog(R.string.request_permission_location_title,
                                    R.string.night_mode_location_permission_message,
                                    //todo redirect to night mode support page or hide more info
                                    (dialog, which) -> UserSupport.showLocationPermissionMoreInfoPage(fragment.getActivity()));
    }

    @Override
    public void showEnableInstructionsDialog() {
        showEnableInstructionsDialog(R.string.request_permission_location_required_title,
                                     R.string.night_mode_location_permission_required_message,
                                     //todo redirect to night mode support page or hide more info
                                     (dialog, which) -> UserSupport.showLocationPermissionMoreInfoPage(fragment.getActivity()));
    }
}
