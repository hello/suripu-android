package is.hello.sense.flows.nightmode.ui.views;

import android.app.Fragment;
import android.support.annotation.NonNull;

import is.hello.sense.Manifest;
import is.hello.sense.R;
import is.hello.sense.permissions.Permission;
import is.hello.sense.util.Constants;

public class NightModeLocationPermission extends Permission {

    public NightModeLocationPermission(@NonNull final Fragment fragment) {
        super(fragment,
              Constants.NONE,
              R.string.action_continue);
    }

    @Override
    protected String getPermissionName() {
        return Manifest.permission.ACCESS_FINE_LOCATION;
    }

    @Override
    public void requestPermissionWithDialog() {
        requestPermissionWithDialog(R.string.request_permission_location_title,
                                    R.string.night_mode_location_permission_message,
                                    null);
    }

    @Override
    public void showEnableInstructionsDialog() {
        showEnableInstructionsDialog(R.string.request_permission_location_required_title,
                                     R.string.night_mode_location_permission_required_message,
                                     null);
    }
}
