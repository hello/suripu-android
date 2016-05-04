package is.hello.sense.permissions;


import android.app.Fragment;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import is.hello.sense.Manifest;
import is.hello.sense.R;
import is.hello.sense.ui.common.UserSupport;

public class LocationPermission extends Permission {
    public LocationPermission(@NonNull final Fragment fragment) {
        super(fragment);
    }

    @Override
    protected String getPermissionName() {
        return Manifest.permission.ACCESS_COARSE_LOCATION;
    }

    @Override
    protected int getPermissionCode() {
        return getPermissionName().hashCode();
    }

    @Override
    public void requestPermissionWithDialog() {
        requestPermissionWithDialog(
                R.string.request_permission_location_title,
                R.string.request_permission_location_message,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UserSupport.showLocationPermissionMoreInfoPage(fragment.getActivity());
                    }
                });
    }

    @Override
    public void showEnableInstructionsDialog() {
        showEnableInstructionsDialog(R.string.request_permission_location_required_title,
                                     R.string.request_permission_location_required_message,
                                     new DialogInterface.OnClickListener() {
                                         @Override
                                         public void onClick(DialogInterface dialog, int which) {
                                             UserSupport.showLocationPermissionMoreInfoPage(fragment.getActivity());
                                         }
                                     });
    }
}
