package is.hello.sense.ui.common;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.permissions.ExternalStoragePermission;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Analytics.ProfilePhoto.Source;
import is.hello.sense.util.Fetch;
import is.hello.sense.util.FilePathUtil;
import is.hello.sense.util.ImageUtil;
import is.hello.sense.util.Logger;
import retrofit.mime.TypedFile;
import rx.schedulers.Schedulers;

import static is.hello.sense.util.Analytics.ProfilePhoto.Source.CAMERA;
import static is.hello.sense.util.Analytics.ProfilePhoto.Source.FACEBOOK;
import static is.hello.sense.util.Analytics.ProfilePhoto.Source.GALLERY;

/**
 * Must be instantiated by {@link is.hello.sense.ui.common.ProfileImageManager.Builder}
 */
public class ProfileImageManager {
    private static final int REQUEST_CODE_PICTURE = 0x30;
    private static final int OPTION_ID_FROM_FACEBOOK = 0;
    private static final int OPTION_ID_FROM_CAMERA = 1;
    private static final int OPTION_ID_FROM_GALLERY = 2;
    private static final int OPTION_ID_REMOVE_PICTURE = 4;

    /**
     * Used instead of null to make Uri <code> @NonNull </code>.
     * It is the equivalent of empty string "".
     */
    private static final Uri EMPTY_URI_STATE = Uri.EMPTY;
    private static final String EMPTY_URI_STATE_STRING = EMPTY_URI_STATE.toString();

    private final Context context;
    private final Fragment fragment;
    private final ImageUtil imageUtil;
    private final FilePathUtil filePathUtil;
    private final ExternalStoragePermission permission;
    private Uri imageUri;
    private String fullImageUriString;
    private Uri tempImageUri;
    private Source imageSource;
    private int optionSelectedId;

    private ProfileImageManager(@NonNull final Fragment fragment,
                               @NonNull final ImageUtil imageUtil,
                               @NonNull final FilePathUtil filePathUtil){
        this.context = fragment.getActivity();
        this.fragment = fragment;
        this.imageUtil = imageUtil;
        this.filePathUtil = filePathUtil;
        this.permission = new ExternalStoragePermission(fragment);
        this.imageUri = EMPTY_URI_STATE;
        this.fullImageUriString = EMPTY_URI_STATE_STRING;
        this.optionSelectedId = -1;
    }

    public void showPictureOptions() {
        //Todo Analytics.trackEvent(Analytics.Backside.EVENT_PICTURE_OPTIONS, null);

        final ArrayList<SenseBottomSheet.Option> options = new ArrayList<>(4);

        // User will always have option to import from facebook b/c they may update their picture outside the app
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

        if(!imageUri.equals(EMPTY_URI_STATE)){
            options.add(
                    new SenseBottomSheet.Option(OPTION_ID_REMOVE_PICTURE)
                            .setTitle(R.string.action_remove_picture)
                            .setTitleColor(ContextCompat.getColor(context, R.color.destructive_accent))
                            .setIcon(R.drawable.icon_alarm_delete)
                       );
        }

        final BottomSheetDialogFragment advancedOptions = BottomSheetDialogFragment.newInstance(options);
        advancedOptions.setTargetFragment(fragment, REQUEST_CODE_PICTURE);
        advancedOptions.showAllowingStateLoss(fragment.getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    /**
     * @return true if requestCode matched and resultCode was Activity.RESULT_OK else false.
     */
    public boolean onActivityResult(final int requestCode, final int resultCode, @NonNull final Intent data) {
        if(resultCode != Activity.RESULT_OK){
            return false;
        }
        boolean wasResultHandled = true;
        if(requestCode == REQUEST_CODE_PICTURE){
            final int optionID = data.getIntExtra(BottomSheetDialogFragment.RESULT_OPTION_ID, -1);
            handlePictureOptionSelection(optionID);
        } else if(requestCode == Fetch.Image.REQUEST_CODE_CAMERA) {
            setImageUriWithTemp();
            setImageSource(CAMERA);
            ((Listener) fragment).onFromCamera(getImageUriString());
        } else if(requestCode == Fetch.Image.REQUEST_CODE_GALLERY){
            final Uri imageUri = data.getData();
            setImageUri(imageUri);
            setImageSource(GALLERY);
            ((Listener) fragment).onFromGallery(getImageUriString());
        } else{
            wasResultHandled = false;
        }
        return wasResultHandled;
    }

    public void setImageUri(@NonNull final Uri uri) {
        imageUri = uri;
        tempImageUri = EMPTY_URI_STATE;
        setFullImageUriString(uri.equals(EMPTY_URI_STATE) ? EMPTY_URI_STATE_STRING : filePathUtil.getRealPath(uri));
    }

    public Uri getImageUri(){
        return imageUri;
    }

    public String getFullImageUriString() { return fullImageUriString; }

    private void setImageUriWithTemp() {
        setImageUri(tempImageUri);
    }

    public String getImageUriString() {
        return imageUri.toString();
    }

    /**
     * Used primarily for giving pictures taken from camera a temporary file location
     */
    private void setTempImageUri(@NonNull final Uri imageUri) {
        tempImageUri = imageUri;
    }

    /**
     * Used primarily to upload local files through api requiring full uri path
     * And facebook to override {@link this#setImageUri(Uri)}
     */
    public void setFullImageUriString(@NonNull final String imageUriString) {
        fullImageUriString = imageUriString;
    }

    /**
     *
     * @param imageSource Used for Segment.io analytics
     */
    public void setImageSource(@NonNull final Source imageSource) {
        this.imageSource = imageSource;
    }

    /**
     *
     * @param id Id of selected option from bottom sheet. Used so correct option action will be triggered
     *           after obtaining permission.
     */
    private void setOptionSelectedId(final int id){
        this.optionSelectedId = id;
    }

    public void prepareImageUpload() {
        if(fullImageUriString != null) {
            prepareImageUpload(fullImageUriString);
        }
    }

    public void prepareImageUpload(@NonNull final String filePath){
        final boolean mustDownload = !filePathUtil.isFoundOnDevice(filePath);
        imageUtil.provideObservableToCompressFile(filePath, mustDownload)
                .doOnNext(file -> {
                    final TypedFile typedFile = new TypedFile("multipart/form-data", file);
                    Logger.warn(ProfileImageManager.class.getSimpleName(), " file size in bytes " + typedFile.length());
                    ((Listener) fragment).onUploadReady(typedFile,imageSource);
                })
                .doOnError(Functions.LOG_ERROR)
                .subscribeOn(Schedulers.io())
                .observeOn(Rx.mainThreadScheduler())
                .subscribe();
    }

    //region permission checks

    public void onRequestPermissionResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (permission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            handlePictureOptionSelection(optionSelectedId);
            optionSelectedId = -1;
        } else {
            permission.showEnableInstructionsDialogForGallery();
        }
    }

    //endregion

    //region Camera Options

    private void handlePictureOptionSelection(final int optionID){
        setOptionSelectedId(optionID);

        switch(optionID){
            case OPTION_ID_FROM_FACEBOOK:
                ((Listener) fragment).onImportFromFacebook();
                setImageSource(FACEBOOK);
                break;
            case OPTION_ID_FROM_CAMERA:
                final File imageFile = imageUtil.createFile(false);
                if(imageFile != null){
                    final Uri imageUri = Uri.fromFile(imageFile);
                    setTempImageUri(imageUri);
                    Fetch.imageFromCamera().fetch(fragment, imageUri);
                }
                break;
            case OPTION_ID_FROM_GALLERY:
                if(permission.isGranted()){
                    Fetch.imageFromGallery().fetch(fragment);
                } else{
                    permission.requestPermission();
                }
                break;
            case OPTION_ID_REMOVE_PICTURE:
                setImageUri(EMPTY_URI_STATE);
                ((Listener) fragment).onRemove();
                break;
            default:
                Logger.warn(ProfileImageManager.class.getSimpleName(), "unknown picture option selected");
        }
    }

    //endregion

    public interface Listener {

        void onImportFromFacebook();

        void onFromCamera(final String imageUriString);

        void onFromGallery(final String imageUriString);

        void onUploadReady(final TypedFile imageFile, final Source source);

        void onRemove();
    }

    public static class Builder {

        private final ImageUtil imageUtil;
        private final FilePathUtil filePathUtil;
        private Fragment fragmentListener;

        public Builder(@NonNull final ImageUtil imageUtil, @NonNull final FilePathUtil filePathUtil){
            this.imageUtil = imageUtil;
            this.filePathUtil = filePathUtil;
        }

        public Builder addFragmentListener(@NonNull final Fragment listener){
            checkFragmentInstance(listener);
            this.fragmentListener = listener;
            return this;
        }

        public ProfileImageManager build(){
            return new ProfileImageManager(fragmentListener, imageUtil, filePathUtil);
        }

        private void checkFragmentInstance(@NonNull final Fragment fragment) {
            if (!(fragment instanceof Listener)) {
                throw new ClassCastException(
                        fragment.toString() + " must implement " + Listener.class.getSimpleName()
                );
            }
        }
    }

}
