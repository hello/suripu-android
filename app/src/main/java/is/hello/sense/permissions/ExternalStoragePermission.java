package is.hello.sense.permissions;

import android.app.Fragment;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import is.hello.sense.Manifest;
import is.hello.sense.R;
import is.hello.sense.ui.common.UserSupport;

public class ExternalStoragePermission extends Permission {

    public ExternalStoragePermission(@NonNull final Fragment fragment) {
        super(fragment);
    }

    @Override
    protected String getPermissionName() {
        return Manifest.permission.WRITE_EXTERNAL_STORAGE;
    }

    @Override
    protected int getPermissionCode() {
        return getPermissionName().hashCode();
    }

    @Override
    public void requestPermissionWithDialog() {
        requestPermissionWithDialog(
                R.string.request_permission_write_external_storage_title,
                R.string.request_permission_write_external_storage_message,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UserSupport.showStoragePermissionMoreInfoPage(fragment.getActivity());
                    }
                });
    }

    @Override
    public void showEnableInstructionsDialog() {
        showEnableInstructionsDialog(R.string.request_permission_write_external_storage_required_title,
                                     R.string.request_permission_write_external_storage_required_message,
                                     new DialogInterface.OnClickListener() {
                                         @Override
                                         public void onClick(DialogInterface dialog, int which) {
                                             UserSupport.showStoragePermissionMoreInfoPage(fragment.getActivity());
                                         }
                                     });
    }
}
