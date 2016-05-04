package is.hello.sense.permissions;

import android.app.Fragment;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.PermissionChecker;

import is.hello.sense.R;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public abstract class Permission {
    protected final Fragment fragment;

    public Permission(@NonNull final Fragment fragment) {
        this.fragment = fragment;
    }

    public boolean isGranted() {
        final int permissionLevel = PermissionChecker.checkSelfPermission(fragment.getActivity(),
                                                                          getPermissionName());
        return permissionLevel == PermissionChecker.PERMISSION_GRANTED;
    }

    public boolean isGrantedFromResult(final int requestCode,
                                       final String[] permissions,
                                       final int[] grantResults) {
        if (permissions == null || grantResults == null || permissions.length == 0 || grantResults.length == 0) {
            return false;
        }
        return (requestCode == getPermissionCode() &&
                permissions.length == 1 && permissions[0].equals(getPermissionName()) &&
                grantResults.length == 1 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED);
    }

    public void requestPermission() {
        FragmentCompat.requestPermissions(fragment,
                                          getPermissions(), getPermissionCode());
    }


    protected void requestPermissionWithDialog(@StringRes final int titleRes,
                                               @StringRes final int messageRes,
                                               @NonNull final DialogInterface.OnClickListener listener) {
        Analytics.trackEvent(Analytics.Permissions.EVENT_WE_NEED_LOCATION, null);

        SenseAlertDialog dialog = new SenseAlertDialog(fragment.getActivity());
        dialog.setTitle(titleRes);
        dialog.setMessage(messageRes);
        dialog.setPositiveButton(R.string.action_continue, (sender, which) -> {
            requestPermission();
        });
        dialog.setNegativeButton(R.string.action_more_info, listener);
        dialog.show();
    }

    /**
     * If the user denies any required permission show this in case they checked "Don't show again"
     * and are no longer able to enable the permission via our app. Will explain how they can enable
     * the permission from outside the app.
     */
    protected void showEnableInstructionsDialog(@StringRes final int titleRes,
                                                @StringRes final int messageRes,
                                                @NonNull final DialogInterface.OnClickListener listener) {
        Analytics.trackEvent(Analytics.Permissions.EVENT_LOCATION_DISABLED, null);

        SenseAlertDialog dialog = new SenseAlertDialog(fragment.getActivity());
        CharSequence clickableText = fragment.getResources().getText(messageRes);
        dialog.setTitle(titleRes);
        dialog.setMessage(Styles.resolveSupportLinks(fragment.getActivity(), clickableText));
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.setNegativeButton(R.string.action_more_info, listener);
        dialog.show();
    }

    public void showEnableInstructionsDialog(@NonNull PermissionDialogResources dialogResources) {
        showEnableInstructionsDialog(dialogResources.dialogTitle(),
                                     dialogResources.dialogMessage(),
                                     dialogResources.clickListener());
    }

    protected int getPermissionCode() {
        return getPermissionName().hashCode();
    }

    private String[] getPermissions() {
        return new String[]{getPermissionName()};
    }

    protected abstract String getPermissionName();

    protected abstract void requestPermissionWithDialog();

    protected abstract void showEnableInstructionsDialog();

    public interface PermissionDialogResources {
        @StringRes
        int dialogTitle();

        @StringRes
        int dialogMessage();

        @NonNull
        DialogInterface.OnClickListener clickListener();
    }

}
