package is.hello.sense.ui.common;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.permissions.ExternalStoragePermission;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Fetch;
import is.hello.sense.util.ImageUtil;
import is.hello.sense.util.Logger;

public class ProfileImageManager {
    private static final int REQUEST_CODE_PICTURE = 0x30;
    private static final int OPTION_ID_FROM_FACEBOOK = 0;
    private static final int OPTION_ID_FROM_CAMERA = 1;
    private static final int OPTION_ID_FROM_GALLERY = 2;
    private static final int OPTION_ID_REMOVE_PICTURE = 4;

    private final Context context;
    private final Fragment fragment;
    private ImageUtil imageUtil;
    private Uri imageUri;
    private Uri tempImageUri;

    public ProfileImageManager(@NonNull final Context context, @NonNull final Fragment fragment, @NonNull ImageUtil imageUtil){
        checkFragmentInstance(fragment);
        this.context = context;
        this.fragment = fragment;
        this.imageUtil = imageUtil;
    }

    public void showPictureOptions() {
        //Todo Analytics.trackEvent(Analytics.Backside.EVENT_PICTURE_OPTIONS, null);

        ArrayList<SenseBottomSheet.Option> options = new ArrayList<>();


        options.add(
                new SenseBottomSheet.Option(OPTION_ID_FROM_FACEBOOK)
                        .setTitle(R.string.action_import_from_facebook)
                        .setTitleColor(ContextCompat.getColor(context, R.color.text_dark))
                        .setIcon(R.drawable.facebook_logo)
                   );

        if(imageUtil.hasDeviceCamera()){
            options.add(
                    new SenseBottomSheet.Option(OPTION_ID_FROM_CAMERA)
                            .setTitle(R.string.action_take_photo)
                            .setTitleColor(ContextCompat.getColor(context, R.color.text_dark))
                            .setIcon(R.drawable.settings_camera)
                       );
        }

        options.add(
                new SenseBottomSheet.Option(OPTION_ID_FROM_GALLERY)
                        .setTitle(R.string.action_import_from_gallery)
                        .setTitleColor(ContextCompat.getColor(context, R.color.text_dark))
                        .setIcon(R.drawable.settings_photo_library)
                   );

        if(imageUri != null){
            options.add(
                    new SenseBottomSheet.Option(OPTION_ID_REMOVE_PICTURE)
                            .setTitle(R.string.action_remove_picture)
                            .setTitleColor(ContextCompat.getColor(context, R.color.destructive_accent))
                            .setIcon(R.drawable.icon_alarm_delete)
                       );
        }

        BottomSheetDialogFragment advancedOptions = BottomSheetDialogFragment.newInstance(options);
        advancedOptions.setTargetFragment(fragment, REQUEST_CODE_PICTURE);
        advancedOptions.showAllowingStateLoss(fragment.getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK) return;
        if(requestCode == REQUEST_CODE_PICTURE){
            final int optionID = data.getIntExtra(BottomSheetDialogFragment.RESULT_OPTION_ID, -1);
            handlePictureOptionSelection(optionID);
        } else if(requestCode == Fetch.Image.REQUEST_CODE_CAMERA) {
            this.setImageUriWithTemp();
            //Todo profilePictureItem.setValue(profileImageManager.getImageUriString());
            ((Listener) fragment).onFromCamera(getImageUriString());
        } else if(requestCode == Fetch.Image.REQUEST_CODE_GALLERY){
            final Uri imageUri = data.getData();
            //Todo profilePictureItem.setValue(imageUri.toString());
            this.setImageUri(imageUri);
            ((Listener) fragment).onFromGallery(getImageUriString());
        }
    }

    public void setImageUri(Uri uri) {
        this.imageUri = uri;
        this.tempImageUri = null;
    }

    public Uri getImageUri(){
        return this.imageUri;
    }

    public void setImageUriWithTemp() {
        setImageUri(tempImageUri);
    }

    public String getImageUriString() {
        return imageUri != null ? imageUri.toString() : "";
    }

    //Used primarily for take picture from camera
    private void setTempImageUri(Uri tempImageUri) {
        this.tempImageUri = tempImageUri;
    }

    //region Camera Options

    private void handlePictureOptionSelection(final int optionID){
        switch(optionID){
            case OPTION_ID_FROM_FACEBOOK:
                ((Listener) fragment).onImportFromFacebook();
                break;
            case OPTION_ID_FROM_CAMERA:
                File imageFile = imageUtil.createFile(true);
                if(imageFile != null){
                    Uri imageUri = Uri.fromFile(imageFile);
                    setTempImageUri(imageUri);
                    Fetch.imageFromCamera().fetch(fragment, imageUri);
                }
                break;
            case OPTION_ID_FROM_GALLERY:
                Fetch.imageFromGallery().fetch(fragment);
                break;
            case OPTION_ID_REMOVE_PICTURE:
                setImageUri(null);
                ((Listener) fragment).onRemove();
            default:
                Logger.warn(ProfileImageManager.class.getSimpleName(), "unknown picture option selected");
        }
    }

    //endregion

    private void checkFragmentInstance(Fragment fragment) {
        if (fragment instanceof Listener == false) {
            throw new ClassCastException(
                    fragment.toString() + " must implement " + Listener.class.getSimpleName()
            );
        }
    }

    public interface Listener {

        void onImportFromFacebook();

        void onFromCamera(String imageUriString);

        void onFromGallery(String imageUriString);

        void onRemove();
    }

}
