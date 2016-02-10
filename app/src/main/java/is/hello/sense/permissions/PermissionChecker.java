package is.hello.sense.permissions;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import is.hello.sense.Manifest;
import is.hello.sense.R;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

@TargetApi(Build.VERSION_CODES.M)
public class PermissionChecker {
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 0x10C;


    // Location Permission Start
    public static boolean needsLocationPermission(@NonNull Fragment fragment) {
        return fragment.getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED;
    }

    public static boolean isLocationPermissionGranted(int requestCode,
                                                      @NonNull String[] permissions,
                                                      @NonNull int[] grantResults) {
        return (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                permissions.length == 1 && permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
    }

    public static void requestLocationPermission(@NonNull Fragment fragment) {
        Analytics.trackEvent(Analytics.Permissions.EVENT_WE_NEED_LOCATION, null);

        SenseAlertDialog dialog = new SenseAlertDialog(fragment.getActivity());
        dialog.setTitle(R.string.request_permission_location_title);
        dialog.setMessage(R.string.request_permission_location_message);
        dialog.setPositiveButton(R.string.action_continue, (sender, which) -> {
            fragment.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        });
        dialog.setNegativeButton(R.string.action_more_info, (sender, which) -> {
            UserSupport.showLocationPermissionMoreInfoPage(fragment.getActivity());
        });
        dialog.show();
    }

    /**
     * If the user denies any required permission show this in case they checked "Don't show again"
     * and are no longer able to enable the permission via our app. Will explain how they can enable
     * the permission from outside the app.
     */
    public static void showEnableInstructionsDialog(@NonNull Fragment fragment) {
        Analytics.trackEvent(Analytics.Permissions.EVENT_LOCATION_DISABLED, null);

        SenseAlertDialog dialog = new SenseAlertDialog(fragment.getActivity());
        CharSequence clickableText = fragment.getResources().getText(R.string.request_permission_required_message);
        dialog.setTitle(R.string.request_permission_required_title);
        dialog.setMessage(Styles.resolveSupportLinks(fragment.getActivity(), clickableText));
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.setNegativeButton(R.string.action_more_info, (sender, which) -> {
            UserSupport.showLocationPermissionMoreInfoPage(fragment.getActivity());
        });

        dialog.show();
    }

    // Location Permission End

}
