package is.hello.sense.ui.common;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.permissions.ExternalStoragePermission;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Fetch;
import is.hello.sense.util.FilePathUtil;
import is.hello.sense.util.ImageUtil;
import is.hello.sense.util.Logger;
import retrofit.mime.TypedFile;
import rx.Observable;
import rx.schedulers.Schedulers;

import static is.hello.sense.util.Analytics.ProfilePhoto.Source.CAMERA;
import static is.hello.sense.util.Analytics.ProfilePhoto.Source.FACEBOOK;
import static is.hello.sense.util.Analytics.ProfilePhoto.Source.GALLERY;
import static is.hello.sense.util.Fetch.Image.REQUEST_CODE_CAMERA;
import static is.hello.sense.util.Fetch.Image.REQUEST_CODE_GALLERY;

/**
 * Must be instantiated by {@link is.hello.sense.ui.common.ProfileImageManager.Builder}
 */
public class ProfileImageManager {
    private static final int REQUEST_CODE_OPTIONS = 5;
    private static final int OPTION_ID_FROM_FACEBOOK = 0;
    private static final int OPTION_ID_FROM_CAMERA = 1;
    private static final int OPTION_ID_FROM_GALLERY = 2;
    private static final int OPTION_ID_REMOVE_PICTURE = 4;

    private static final String PREFS = "profile_image_manager_prefs";
    private static final String PHOTO_URI = "PHOTO_URI";

    private final int minOptions;
    private final Fragment fragment;
    private final ImageUtil imageUtil;
    private final FilePathUtil filePathUtil;
    private final ExternalStoragePermission permission;
    private final ArrayList<SenseBottomSheet.Option> options = new ArrayList<>();
    private final SenseBottomSheet.Option deleteOption;
    private final SharedPreferences sharedPreferences;

    private boolean showOptions;
    private Analytics.ProfilePhoto.Source source = null;

    private ProfileImageManager(@NonNull final Fragment fragment,
                                @NonNull final ImageUtil imageUtil,
                                @NonNull final FilePathUtil filePathUtil) {
        final Context context = fragment.getActivity();
        this.sharedPreferences = context.getSharedPreferences(PREFS, 0);
        this.fragment = fragment;
        this.imageUtil = imageUtil;
        this.filePathUtil = filePathUtil;
        this.permission = new ExternalStoragePermission(fragment);
        this.showOptions = true;
        options.add(
                new SenseBottomSheet.Option(OPTION_ID_FROM_FACEBOOK)
                        .setTitle(R.string.action_import_from_facebook)
                        .setTitleColor(ContextCompat.getColor(context, R.color.text_dark))
                        .setIcon(R.drawable.facebook_logo)
                   );
        if (imageUtil.hasDeviceCamera()) {
            options.add(
                    new SenseBottomSheet.Option(OPTION_ID_FROM_CAMERA)
                            .setTitle(R.string.action_take_photo)
                            .setTitleColor(ContextCompat.getColor(context, R.color.text_dark))
                            .setIcon(R.drawable.settings_camera)
                       );
            minOptions = 3;
        } else {
            minOptions = 2;
        }
        options.add(
                new SenseBottomSheet.Option(OPTION_ID_FROM_GALLERY)
                        .setTitle(R.string.action_import_from_gallery)
                        .setTitleColor(ContextCompat.getColor(context, R.color.text_dark))
                        .setIcon(R.drawable.settings_photo_library)
                   );
        deleteOption = new SenseBottomSheet.Option(OPTION_ID_REMOVE_PICTURE)
                .setTitle(R.string.action_remove_picture)
                .setTitleColor(ContextCompat.getColor(context, R.color.destructive_accent))
                .setIcon(R.drawable.icon_alarm_delete);
    }

    public void showPictureOptions() {
        if (!showOptions) {
            return;
        }
        final BottomSheetDialogFragment advancedOptions = BottomSheetDialogFragment.newInstance(options);
        advancedOptions.setTargetFragment(fragment, REQUEST_CODE_OPTIONS);
        advancedOptions.showAllowingStateLoss(fragment.getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    public void hidePictureOptions(){
        final BottomSheetDialogFragment bottomSheet = (BottomSheetDialogFragment) fragment.getFragmentManager()
                                                    .findFragmentByTag(BottomSheetDialogFragment.TAG);
        if(bottomSheet != null){
            bottomSheet.dismissSafely();
        }
    }

    public void addDeleteOption() {
        if (options.size() == minOptions) {
            options.add(deleteOption);
        }
    }

    public void removeDeleteOption() {
        if (options.size() == minOptions + 1) {
            options.remove(deleteOption);
        }
    }

    /**
     * @return true if requestCode matched and resultCode.
     */
    public boolean onActivityResult(final int requestCode, final int resultCode, @NonNull final Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAMERA || requestCode == REQUEST_CODE_GALLERY) {
                setShowOptions(true);
                return true;
            } else {
                return false;
            }
        }

        if (requestCode == REQUEST_CODE_OPTIONS) {
            setShowOptions(false);
            final int optionID = data.getIntExtra(BottomSheetDialogFragment.RESULT_OPTION_ID, -1);
            handlePictureOptionSelection(optionID);
        } else if (requestCode == REQUEST_CODE_CAMERA) {
            final String photoUriString = sharedPreferences.getString(PHOTO_URI, null);
            if (photoUriString == null) {
                return false;
            }
            source = CAMERA; // incase the user took a few days to return.
            final Uri takePhotoUri = Uri.parse(photoUriString);
            getListener().onFromCamera(takePhotoUri);
        } else if (requestCode == REQUEST_CODE_GALLERY) {
            source = GALLERY; // doesn't hurt to be safe
            final Uri imageUri = data.getData();
            getListener().onFromGallery(imageUri);
        } else {
            return false;
        }
        return true;
    }

    public void setShowOptions(final boolean showOptions) {
        this.showOptions = showOptions;
    }

    private Observable<TypedFile> compressImageObservable(@Nullable final Uri imageUri) {
        if (imageUri == null || imageUri.equals(Uri.EMPTY)) {
            return Observable.create((subscriber) -> subscriber.onError(new Throwable("No valid filePath given")));
        }

        final String localPath = filePathUtil.getLocalPath(imageUri);
        final boolean mustDownload = !filePathUtil.isFoundOnDevice(localPath);
        final String path = mustDownload ? imageUri.toString() : localPath;

        return imageUtil.provideObservableToCompressFile(path, mustDownload)
                        .map(file -> new TypedFile("multipart/form-data", file))
                        .doOnError(Functions.LOG_ERROR)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Rx.mainThreadScheduler());
    }

    public void trimCache() {
        imageUtil.trimCache();
    }

    //region permission checks

    public void onRequestPermissionResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (permission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            handleGalleryOption();
        } else {
            permission.showEnableInstructionsDialogForGallery();
        }
    }

    //endregion

    //region Camera Options

    private void handlePictureOptionSelection(final int optionID) {
        switch (optionID) {
            case OPTION_ID_FROM_FACEBOOK:
                handleFacebookOption();
                break;
            case OPTION_ID_FROM_CAMERA:
                handleCameraOption();
                break;
            case OPTION_ID_FROM_GALLERY:
                handleGalleryOption();
                break;
            case OPTION_ID_REMOVE_PICTURE:
                handleRemoveOption();
                break;
            default:
                Logger.warn(ProfileImageManager.class.getSimpleName(), "unknown picture option selected");
        }
    }

    private void handleFacebookOption() {
        source = FACEBOOK;
        getListener().onImportFromFacebook();
    }

    private void handleCameraOption() {
        final File imageFile = imageUtil.createFile(false);
        if (imageFile != null) {
            source = CAMERA;
            final Uri takePhotoUri = Uri.fromFile(imageFile);
            sharedPreferences.edit().putString(PHOTO_URI, takePhotoUri.toString()).apply();
            Fetch.imageFromCamera().fetch(fragment, takePhotoUri);
        }
    }

    private void handleGalleryOption() {
        if (permission.isGranted()) {
            source = GALLERY;
            Fetch.imageFromGallery().fetch(fragment);
        } else {
            permission.requestPermission();
        }
    }

    private void handleRemoveOption() {
        getListener().onRemove();
    }

    //endregion

    public void compressImage(@Nullable final Uri imageUri) {
        compressImageObservable(imageUri)
                .subscribe(this::compressImageSuccess,
                           this::compressImageError);
    }

    private void compressImageSuccess(@NonNull final TypedFile compressedFile) {
        getListener().onImageCompressedSuccess(compressedFile, source);

    }

    private void compressImageError(@NonNull final Throwable e) {
        getListener().onImageCompressedError(e, R.string.error_internet_connection_generic_title,  R.string.error_account_upload_photo_message);
    }

    private Listener getListener() {
        return ((Listener) fragment);
    }

    public interface Listener {

        void onImportFromFacebook();

        void onFromCamera(@NonNull final Uri imageUri);

        void onFromGallery(@NonNull final Uri imageUri);

        void onRemove();

        void onImageCompressedSuccess(@NonNull TypedFile compressedImage, @NonNull Analytics.ProfilePhoto.Source source);

        void onImageCompressedError(@NonNull Throwable e, @StringRes int titleRes, @StringRes int messageRes);

    }

    public static class Builder {

        private final ImageUtil imageUtil;
        private final FilePathUtil filePathUtil;
        private Fragment fragmentListener;

        public Builder(@NonNull final ImageUtil imageUtil, @NonNull final FilePathUtil filePathUtil) {
            this.imageUtil = imageUtil;
            this.filePathUtil = filePathUtil;
        }

        public Builder addFragmentListener(@NonNull final Fragment listener) {
            checkFragmentInstance(listener);
            this.fragmentListener = listener;
            return this;
        }

        public ProfileImageManager build() {
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
