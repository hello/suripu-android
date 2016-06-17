package is.hello.sense.api.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import is.hello.sense.util.Analytics;
import retrofit.mime.TypedFile;

public class ProfileImage implements Parcelable {

    /**
     * Used instead of null to make Uri <code> @NonNull </code>.
     * It is the equivalent of empty string "".
     */
    private static final Uri EMPTY_URI_STATE = Uri.EMPTY;

    private Analytics.ProfilePhoto.Source source;

    private Uri imageUri;

    private Uri tempImageUri;

    public static ProfileImage createDefault() {
        return new ProfileImage();
    }

    private ProfileImage(){
        this.imageUri = EMPTY_URI_STATE;
        this.tempImageUri = EMPTY_URI_STATE;
    }

    protected ProfileImage(final Parcel in) {
        source = in.readParcelable(Analytics.ProfilePhoto.Source.class.getClassLoader());
        imageUri = in.readParcelable(Uri.class.getClassLoader());
        tempImageUri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<ProfileImage> CREATOR = new Creator<ProfileImage>() {
        @Override
        public ProfileImage createFromParcel(final Parcel in) {
            return new ProfileImage(in);
        }

        @Override
        public ProfileImage[] newArray(final int size) {
            return new ProfileImage[size];
        }
    };

    public Analytics.ProfilePhoto.Source getSource() {
        return source;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(@NonNull final Uri uri) {
        imageUri = uri;
        tempImageUri = EMPTY_URI_STATE;
    }

    public void setEmptyUriState(){
        setImageUri(EMPTY_URI_STATE);
    }

    public void setImageUriWithTemp() {
        setImageUri(tempImageUri);
    }

    public String getImageUriString() {
        return imageUri.toString();
    }

    /**
     * Used primarily for giving pictures taken from camera a temporary file location
     */
    public void setTempImageUri(@NonNull final Uri imageUri) {
        tempImageUri = imageUri;
    }

    /**
     *
     * @param imageSource Used for Segment.io analytics
     */
    public void setImageSource(@NonNull final Analytics.ProfilePhoto.Source imageSource) {
        this.source = imageSource;
    }


    public boolean hasImageUri() {
        return !EMPTY_URI_STATE.equals(imageUri);
    }

    @Override
    public String toString() {
        return "ProfileImage{" +
                "source=" + source +
                ", imageUri=" + imageUri +
                ", tempImageUri=" + tempImageUri +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(source, flags);
        dest.writeParcelable(imageUri, flags);
        dest.writeParcelable(tempImageUri, flags);
    }

    public static class UploadReady{

        private final TypedFile file;
        private final Analytics.ProfilePhoto.Source source;

        public UploadReady(@NonNull final TypedFile file, @NonNull final Analytics.ProfilePhoto.Source source){
            this.file = file;
            this.source = source;
        }

        public TypedFile getFile() {
            return file;
        }

        public Analytics.ProfilePhoto.Source getSource() {
            return source;
        }

        @Override
        public String toString() {
            return "UploadReady{" +
                    "file=" + file +
                    ", source=" + source +
                    '}';
        }
    }
}
