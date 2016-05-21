package is.hello.sense.api.fb.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class FacebookProfilePicture extends ApiResponse{

    @SerializedName("data")
    private final Data imageData;

    public FacebookProfilePicture(@NonNull final String imageUrl){
        this.imageData = new Data(imageUrl);
    }

    public String getImageUrl() {
        return imageData.getImageUrl();
    }


    public static class Data extends ApiResponse{
        @SerializedName("url")
        private final String imageUrl;

        public Data(@NonNull final String imageUrl){
            this.imageUrl = imageUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

    }

}
